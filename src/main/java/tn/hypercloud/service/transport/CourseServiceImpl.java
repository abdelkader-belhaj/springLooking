package tn.hypercloud.service.transport;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.hypercloud.entity.transport.*;
import tn.hypercloud.entity.transport.enums.*;
import tn.hypercloud.repository.transport.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class CourseServiceImpl implements ICourseService {

    private final CourseRepository courseRepository;
    private final PaiementRepository paiementRepository;
    private final IDistanceService distanceService;// ton service OSRM
    private final ChauffeurRepository chauffeurRepository;
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
        return courseRepository.findByChauffeur_IdChauffeur(chauffeurId);
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
    public Course startCourse(Long id) {
        return updateStatut(id, CourseStatus.STARTED);
    }

    @Override
    @Transactional
    public Course completeCourse(Long id) {
        Course course = getCourseById(id);
        if (course == null) return null;

        // Calcul prix final
        IDistanceService.RouteInfo route = distanceService.calculateRoute(
                course.getLocalisationDepart(), course.getLocalisationArrivee());
        BigDecimal prixFinal = calculateFinalPrice(route, course.getVehicule());
        course.setPrixFinal(prixFinal);

        // Création paiement (commission 20 % automatique via @PrePersist)
        PaiementTransport paiementTransport = PaiementTransport.builder()
                .course(course)
                .montantTotal(prixFinal)
                .methode(PaiementMethode.CARD)
                .statut(PaiementStatut.COMPLETED)
                .datePaiement(LocalDateTime.now())
                .build();

        paiementTransport = paiementRepository.save(paiementTransport);

        // === MISE À JOUR SOLDE CHAUFFEUR + TRANSACTION ===
        BigDecimal montantNet = paiementTransport.getMontantNet();
        Chauffeur chauffeur = course.getChauffeur();

// Protection null-safe + mise à jour du solde
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

        // Mise à jour course
        course.setPaiementTransport(paiementTransport);
        course.setMontantCommission(paiementTransport.getMontantCommission());
        course.setStatut(CourseStatus.COMPLETED);

        return courseRepository.save(course);
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

        Localisation pickup = demande.getLocalisationDepart();

        // Chauffeurs disponibles + approuvés
        List<Chauffeur> candidates = chauffeurRepository.findByDisponibiliteAndStatut(
                DisponibiliteStatut.AVAILABLE, ChauffeurStatut.ACTIVE);

        // Filtre + trie par distance réelle
        List<Chauffeur> nearbyDrivers = candidates.stream()
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
    @Override
    public List<Course> getCoursesByClient(Long clientId) {
        return courseRepository.findByDemande_Client_Id(clientId);
    }
}
