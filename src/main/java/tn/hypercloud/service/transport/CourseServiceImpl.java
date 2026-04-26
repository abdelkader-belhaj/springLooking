package tn.hypercloud.service.transport;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.hypercloud.dto.transport.PaymentVerificationStatusDto;
import tn.hypercloud.entity.transport.*;
import tn.hypercloud.entity.transport.enums.*;
import tn.hypercloud.repository.transport.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.concurrent.ThreadLocalRandom;
import java.util.List;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class CourseServiceImpl implements ICourseService {

    private final CourseRepository courseRepository;
    private final PaiementRepository paiementRepository;
    private final IPaiementService paiementService;
    private final IDistanceService distanceService;// ton service OSRM
    private final ChauffeurRepository chauffeurRepository;
    private final VehiculeRepository vehiculeRepository;
    private final WalletTransactionRepository walletTransactionRepository;
    private final MatchingRepository matchingRepository;

    @Override
    public Course addCourse(Course course) {
        return courseRepository.save(course);
    }

    @Override
    public Course updateCourse(Course course) {
        return courseRepository.save(course);
    }

    @Override
    public void deleteCourse(Long id) {
        courseRepository.deleteById(id);
    }

    @Override
    public Course getCourseById(Long id) {
        return courseRepository.findById(id).orElse(null);
    }

    @Override
    public List<Course> getAllCourses() {
        return courseRepository.findAll();
    }

    @Override
    public List<Course> getCoursesByStatut(CourseStatus statut) {
        return courseRepository.findByStatut(statut);
    }

    @Override
    public List<Course> getCoursesByChauffeur(Long chauffeurId) {
        return courseRepository.findHistoryByChauffeurId(chauffeurId);
    }

    @Override
    @Transactional
    public Course updateStatut(Long id, CourseStatus statut) {
        Course course = getCourseById(id);
        if (course == null) return null;
        course.setStatut(statut);
        return courseRepository.save(course);
    }

    @Override
    @Transactional
    public Course startCourse(Long id) {
        Course course = getCourseById(id);
        if (course == null) {
            return null;
        }

        DemandeCourse demande = course.getDemande();
        if (demande != null
                && demande.getStatut() == DemandeStatus.ACCEPTED
                && Boolean.TRUE.equals(demande.getApprobationClientRequise())) {
            throw new IllegalStateException("Le client doit confirmer la course acceptee avant le demarrage");
        }

        course.setStatut(CourseStatus.STARTED);

        // La pré-autorisation d'annulation n'est utile qu'en phase ACCEPTED.
        if (demande != null) {
            demande.setPrixClientAccepte(Boolean.FALSE);
            demande.setApprobationClientRequise(Boolean.FALSE);
        }

        return courseRepository.save(course);
    }

    @Override
    @Transactional
    public Course completeCourse(Long id) {
        Course course = getCourseById(id);
        if (course == null) return null;

        // Calcul prix final
        BigDecimal prixFinal = course.getDemande() != null
                ? course.getDemande().getPrixEstime()
                : null;

        if (prixFinal == null || prixFinal.compareTo(BigDecimal.ZERO) <= 0) {
// fallback de sécurité si demande.prixEstime absent
            IDistanceService.RouteInfo route = distanceService.calculateRoute(
                    course.getLocalisationDepart(), course.getLocalisationArrivee());
            prixFinal = calculateFinalPrice(route, course.getVehicule());
        }

        course.setPrixFinal(prixFinal);

        // Sécurité paiement: on termine la course mais on ne crée PAS le paiement ici.
        // Le paiement est créé uniquement après:
        // 1) confirmation client
        // 2) validation chauffeur par code
        course.setPaymentClientConfirmed(false);
        course.setPaymentVerifiedByDriver(false);
        course.setPaymentVerificationCode(null);
        course.setPaymentIntentId(null);
        course.setPaymentClientConfirmedAt(null);
        course.setPaymentVerifiedAt(null);
        course.setStatut(CourseStatus.COMPLETED);

        return courseRepository.save(course);
    }

    @Override
    @Transactional
    public PaymentVerificationStatusDto confirmClientPayment(Long courseId, String paymentIntentId) {
        Course course = getCourseById(courseId);
        if (course == null) {
            throw new RuntimeException("Course non trouvée: " + courseId);
        }

        if (course.getStatut() == CourseStatus.CANCELLED) {
            throw new IllegalStateException("Paiement impossible: la course est annulée");
        }

        if (course.getStatut() != CourseStatus.COMPLETED) {
            throw new IllegalStateException("Le paiement client n'est possible qu'après course terminée");
        }

        if (course.getPaiementTransport() != null) {
            return toPaymentStatusDto(course);
        }

        if (course.getPaymentVerificationCode() == null || course.getPaymentVerificationCode().isBlank()) {
            course.setPaymentVerificationCode(generateVerificationCode());
        }

        course.setPaymentClientConfirmed(true);
        course.setPaymentClientConfirmedAt(LocalDateTime.now());

        if (paymentIntentId != null && !paymentIntentId.isBlank()) {
            course.setPaymentIntentId(paymentIntentId);
        }

        Course saved = courseRepository.save(course);
        return toPaymentStatusDto(saved);
    }

    @Override
    @Transactional
    public PaymentVerificationStatusDto verifyPaymentByDriver(Long courseId, String verificationCode) {
        Course course = getCourseById(courseId);
        if (course == null) {
            throw new RuntimeException("Course non trouvée: " + courseId);
        }

        if (course.getStatut() == CourseStatus.CANCELLED) {
            throw new IllegalStateException("Validation impossible: la course est annulée");
        }

        if (!course.isPaymentClientConfirmed()) {
            throw new IllegalStateException("Paiement client non confirmé");
        }

        if (verificationCode == null || verificationCode.isBlank()) {
            throw new IllegalArgumentException("Code de vérification requis");
        }

        String expectedCode = course.getPaymentVerificationCode();
        if (expectedCode == null || !expectedCode.equals(verificationCode.trim())) {
            throw new IllegalStateException("Code de vérification invalide");
        }

        course.setPaymentVerifiedByDriver(true);
        course.setPaymentVerifiedAt(LocalDateTime.now());

        if (course.getPaiementTransport() == null) {
            createPaymentAfterVerification(course);
        }

        Course saved = courseRepository.save(course);
        return toPaymentStatusDto(saved);
    }

    @Override
    public PaymentVerificationStatusDto getPaymentVerificationStatus(Long courseId) {
        Course course = getCourseById(courseId);
        if (course == null) {
            throw new RuntimeException("Course non trouvée: " + courseId);
        }

        return toPaymentStatusDto(course);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isClientConfirmationReceived(Long courseId) {
        Course course = getCourseById(courseId);
        if (course == null) {
            throw new RuntimeException("Course non trouvée: " + courseId);
        }

        DemandeCourse demande = course.getDemande();
        if (demande == null) {
            return false;
        }

        return Boolean.FALSE.equals(demande.getApprobationClientRequise());
    }

    private void createPaymentAfterVerification(Course course) {
        PaiementTransport paiementTransport = paiementService.createCompletedCoursePayment(
            course,
            PaiementMethode.CARD
        );

        BigDecimal montantNet = paiementTransport.getMontantNet();
        Chauffeur chauffeur = course.getChauffeur();

        BigDecimal nouveauSolde = (chauffeur.getSolde() != null ? chauffeur.getSolde() : BigDecimal.ZERO)
                .add(montantNet);
        chauffeur.setSolde(nouveauSolde);
        chauffeurRepository.save(chauffeur);

        walletTransactionRepository.save(WalletTransaction.builder()
                .chauffeur(chauffeur)
                .montant(montantNet)
                .type(TransactionType.CREDIT_COURSE)
                .description("Course #" + course.getIdCourse())
                .paiementTransport(paiementTransport)
                .build());

        course.setPaiementTransport(paiementTransport);
        course.setMontantCommission(paiementTransport.getMontantCommission());
    }

    private PaymentVerificationStatusDto toPaymentStatusDto(Course course) {
        AnnulationTransport annulation = course.getAnnulationTransport();
        PaiementTransport paiement = course.getPaiementTransport();
        BigDecimal montantBrut = course.getPrixFinal() != null ? course.getPrixFinal() : BigDecimal.ZERO;
        BigDecimal montantPreautorise = BigDecimal.ZERO;

        if (course.getDemande() != null && course.getDemande().getPrixPropose() != null) {
            montantPreautorise = course.getDemande().getPrixPropose();
        }

        if (montantPreautorise.compareTo(BigDecimal.ZERO) < 0) {
            montantPreautorise = BigDecimal.ZERO;
        }

        if (montantPreautorise.compareTo(montantBrut) > 0) {
            montantPreautorise = montantBrut;
        }

        BigDecimal montantRestant = paiement != null
                ? paiement.getMontantTotal()
                : montantBrut.subtract(montantPreautorise).max(BigDecimal.ZERO);

        return PaymentVerificationStatusDto.builder()
                .courseId(course.getIdCourse())
                .clientConfirmed(course.isPaymentClientConfirmed())
                .driverVerified(course.isPaymentVerifiedByDriver())
            .paymentCreated(paiement != null)
            .cancelled(course.getStatut() == CourseStatus.CANCELLED)
                .verificationCode(course.getPaymentVerificationCode())
                .paymentIntentId(course.getPaymentIntentId())
            .paymentStatut(paiement != null ? paiement.getStatut() : null)
            .montantBrut(montantBrut)
            .montantPreautorise(montantPreautorise)
            .montantRestant(montantRestant)
            .penaltyAmount(annulation != null ? annulation.getMontantPenalite() : BigDecimal.ZERO)
            .refundAmount(annulation != null ? annulation.getMontantRemboursement() : BigDecimal.ZERO)
            .cancelledBy(annulation != null ? annulation.getAnnulePar() : null)
            .cancellationReason(annulation != null ? annulation.getRaison() : null)
                .clientConfirmedAt(course.getPaymentClientConfirmedAt())
                .driverVerifiedAt(course.getPaymentVerifiedAt())
                .build();
    }

    private String generateVerificationCode() {
        int code = ThreadLocalRandom.current().nextInt(100000, 1000000);
        return String.valueOf(code);
    }
    private BigDecimal calculateFinalPrice(IDistanceService.RouteInfo route, Vehicule vehicule) {
        BigDecimal baseFee = new BigDecimal("5.00");

        BigDecimal distanceCost = vehicule.getPrixKm()
                .multiply(BigDecimal.valueOf(route.distanceKm()));

        BigDecimal timeCost = vehicule.getPrixMinute()
                .multiply(BigDecimal.valueOf(route.durationMinutes()));

        return baseFee.add(distanceCost).add(timeCost)
                .setScale(2, RoundingMode.HALF_UP);
    }
    // ====================== CALCUL DISTANCE (Haversine) ======================
    private double calculateHaversineDistance(BigDecimal lat1, BigDecimal lon1, BigDecimal lat2, BigDecimal lon2) {
        final int EARTH_RADIUS = 6371; // km
        double dLat = Math.toRadians(lat2.subtract(lat1).doubleValue());
        double dLon = Math.toRadians(lon2.subtract(lon1).doubleValue());
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1.doubleValue())) * Math.cos(Math.toRadians(lat2.doubleValue())) *
                        Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return EARTH_RADIUS * c;
    }

    // ====================== NOUVEAU MATCHING PAR PROXIMITÉ ======================
    @Transactional
    public List<Matching> createProximityMatchings(DemandeCourse demande, double maxDistanceKm) {
        if (demande.getLocalisationDepart() == null ||
                demande.getLocalisationDepart().getLatitude() == null ||
                demande.getLocalisationDepart().getLongitude() == null) {
            throw new IllegalArgumentException("Localisation de départ requise pour le matching");
        }

        if (demande.getTypeVehiculeDemande() == null) {
            throw new IllegalArgumentException("Type de véhicule demandé requis pour le matching");
        }

        Localisation pickup = demande.getLocalisationDepart();
        TypeVehicule requestedType = demande.getTypeVehiculeDemande();

        // Chauffeurs disponibles + approuvés
        List<Chauffeur> candidates = chauffeurRepository.findByDisponibiliteAndStatut(
                DisponibiliteStatut.AVAILABLE, ChauffeurStatut.ACTIVE);

        // Filtre + trie par distance réelle
        List<Chauffeur> nearbyDrivers = candidates.stream()
                .filter(ch -> hasActiveVehicleOfType(ch, requestedType))
                .filter(ch -> ch.getPositionActuelle() != null)
                .filter(ch -> {
                    Localisation driverPos = ch.getPositionActuelle();
                    double distance = calculateHaversineDistance(
                            pickup.getLatitude(), pickup.getLongitude(),
                            driverPos.getLatitude(), driverPos.getLongitude());
                    return distance <= maxDistanceKm;
                })
                .sorted((c1, c2) -> {
                    double dist1 = calculateHaversineDistance(pickup.getLatitude(), pickup.getLongitude(),
                            c1.getPositionActuelle().getLatitude(), c1.getPositionActuelle().getLongitude());
                    double dist2 = calculateHaversineDistance(pickup.getLatitude(), pickup.getLongitude(),
                            c2.getPositionActuelle().getLatitude(), c2.getPositionActuelle().getLongitude());
                    return Double.compare(dist1, dist2);
                })
                .limit(5) // Top 5 plus proches
                .collect(Collectors.toList());

        // Création des Matching
        List<Matching> matchings = nearbyDrivers.stream()
                .map(driver -> Matching.builder()
                        .demande(demande)
                        .chauffeur(driver)
                        .statut(MatchingStatut.PROPOSED)
                        .build())
                .collect(Collectors.toList());

        return matchingRepository.saveAll(matchings);
    }

    private boolean hasActiveVehicleOfType(Chauffeur chauffeur, TypeVehicule requestedType) {
        if (chauffeur == null || chauffeur.getIdChauffeur() == null || requestedType == null) {
            return false;
        }

        return vehiculeRepository
                .findByChauffeur_IdChauffeurAndStatut(chauffeur.getIdChauffeur(), VehiculeStatut.ACTIVE)
                .stream()
                .anyMatch(v -> requestedType.equals(v.getTypeVehicule()));
    }
    @Override
    public List<Course> getCoursesByClient(Long clientId) {
        return courseRepository.findByDemande_Client_Id(clientId);
    }
}
