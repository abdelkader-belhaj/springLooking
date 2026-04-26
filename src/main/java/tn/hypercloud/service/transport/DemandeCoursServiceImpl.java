package tn.hypercloud.service.transport;

import lombok.AllArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.hypercloud.controller.transport.RealTimeTransportController;
import tn.hypercloud.dto.transport.DemandePreauthResponseDto;
import tn.hypercloud.dto.transport.DriverNotificationDTO;
import tn.hypercloud.entity.transport.Course;
import tn.hypercloud.entity.transport.DemandeCourse;
import tn.hypercloud.entity.transport.Localisation;
import tn.hypercloud.entity.transport.Matching;
import tn.hypercloud.entity.transport.enums.ConfirmationClientStatut;
import tn.hypercloud.entity.transport.enums.DemandeStatus;
import tn.hypercloud.entity.transport.enums.MatchingStatut;
import tn.hypercloud.repository.transport.CourseRepository;
import tn.hypercloud.repository.transport.MatchingRepository;
import tn.hypercloud.entity.user.User;
import tn.hypercloud.repository.transport.DemandeCoursRepository;
import tn.hypercloud.repository.transport.LocalisationRepository;
import tn.hypercloud.repository.user.UserRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

@Service
@AllArgsConstructor
public class DemandeCoursServiceImpl implements IDemandeCoursService {

    private static final BigDecimal PENALTY_PREAUTH_RATIO = new BigDecimal("0.20");

    private final DemandeCoursRepository demandeCoursRepository;
    private final CourseRepository courseRepository;
    private final MatchingRepository matchingRepository;
    private final UserRepository userRepository;
    private final LocalisationRepository localisationRepository;
    private final IMatchingService matchingService;        // ← INJECTION
    private final RealTimeTransportController realTimeController;
    private final SimpMessagingTemplate messagingTemplate;

    // ====================== ADD (création par client) ======================
    @Override
    @Transactional
    public DemandeCourse addDemandeCourse(DemandeCourse demandeCourse) {
        // 1. Récupérer le client complet
        if (demandeCourse.getClient() != null && demandeCourse.getClient().getId() != null) {
            User client = userRepository.findById(demandeCourse.getClient().getId())
                    .orElseThrow(() -> new RuntimeException("Client non trouvé avec id: " + demandeCourse.getClient().getId()));
            demandeCourse.setClient(client);
        } else {
            throw new IllegalArgumentException("Client ID est obligatoire");
        }

        // 2. Récupérer localisation départ
        if (demandeCourse.getLocalisationDepart() != null && demandeCourse.getLocalisationDepart().getIdLocalisation() != null) {
            Localisation depart = localisationRepository.findById(demandeCourse.getLocalisationDepart().getIdLocalisation())
                    .orElseThrow(() -> new RuntimeException("Localisation départ non trouvée"));
            demandeCourse.setLocalisationDepart(depart);
        } else {
            throw new IllegalArgumentException("Localisation départ ID obligatoire");
        }

        // 3. Récupérer localisation arrivée
        if (demandeCourse.getLocalisationArrivee() != null && demandeCourse.getLocalisationArrivee().getIdLocalisation() != null) {
            Localisation arrivee = localisationRepository.findById(demandeCourse.getLocalisationArrivee().getIdLocalisation())
                    .orElseThrow(() -> new RuntimeException("Localisation arrivée non trouvée"));
            demandeCourse.setLocalisationArrivee(arrivee);
        } else {
            throw new IllegalArgumentException("Localisation arrivée ID obligatoire");
        }

        // Statut initial = PENDING (comme dans le PDF)
        demandeCourse.setStatut(DemandeStatus.PENDING);

        // Calcul du prix estimé (version simple pour l'instant)
        // TODO : à améliorer avec vraie distance + tarif véhicule
        if (demandeCourse.getPrixEstime() == null) {
            demandeCourse.setPrixEstime(new java.math.BigDecimal("25.00")); // placeholder
        }

        DemandeCourse saved = demandeCoursRepository.save(demandeCourse);
        return saved;
    }

    // ====================== CRUD ======================
    @Override
    public DemandeCourse updateDemandeCourse(DemandeCourse demandeCourse) {
        return demandeCoursRepository.save(demandeCourse);
    }

    @Override
    public void deleteDemandeCourse(Long id) {
        demandeCoursRepository.deleteById(id);
    }

    @Override
    public DemandeCourse getDemandeCoursById(Long id) {
        DemandeCourse demande = demandeCoursRepository.findDetailedById(id).orElse(null);
        enrichClientFacingDetails(demande);
        return demande;
    }

    @Override
    public List<DemandeCourse> getAllDemandeCourses() {
        List<DemandeCourse> demandes = demandeCoursRepository.findAll();
        demandes.forEach(this::enrichClientFacingDetails);
        return demandes;
    }

    @Override
    public List<DemandeCourse> getDemandesByStatut(DemandeStatus statut) {
        List<DemandeCourse> demandes = demandeCoursRepository.findByStatut(statut);
        demandes.forEach(this::enrichClientFacingDetails);
        return demandes;
    }

    @Override
    public DemandeCourse updateStatut(Long id, DemandeStatus statut) {
        DemandeCourse demande = getDemandeCoursById(id);
        if (demande == null) return null;
        demande.setStatut(statut);

        if (statut == DemandeStatus.CANCELLED && demande.getCourse() != null) {
            demande.getCourse().setStatut(tn.hypercloud.entity.transport.enums.CourseStatus.CANCELLED);
            courseRepository.save(demande.getCourse());
        }

        DemandeCourse saved = demandeCoursRepository.save(demande);
        enrichClientFacingDetails(saved);
        return saved;
    }

    // ====================== WORKFLOW MATCHING ======================
    @Override
    @Transactional
    public DemandeCourse startMatching(Long id) {
        DemandeCourse demande = getDemandeCoursById(id);
        if (demande == null) {
            throw new RuntimeException("Demande de course non trouvée");
        }

        if (demande.getStatut() != DemandeStatus.PENDING) {
            throw new IllegalStateException("Seules les demandes en statut PENDING peuvent passer en MATCHING");
        }

        if (!Boolean.TRUE.equals(demande.getPrixClientAccepte())) {
            throw new IllegalStateException("Pré-autorisation paiement requise avant le matching");
        }

        BigDecimal holdAmount = demande.getPrixPropose() != null ? demande.getPrixPropose() : BigDecimal.ZERO;
        BigDecimal estimatedAmount = demande.getPrixEstime() != null ? demande.getPrixEstime() : BigDecimal.ZERO;
        BigDecimal minimumRequiredHold = estimatedAmount.multiply(PENALTY_PREAUTH_RATIO).setScale(2, RoundingMode.HALF_UP);
        if (holdAmount.compareTo(minimumRequiredHold) < 0) {
            throw new IllegalStateException("Montant pré-autorisé insuffisant: minimum requis = 20% du prix estimé");
        }

        // 1. Passage en MATCHING
        demande.setStatut(DemandeStatus.MATCHING);
        demande.setDateModification(LocalDateTime.now());
        DemandeCourse saved = demandeCoursRepository.save(demande);

        // 2. Déclenchement du broadcast (création des N Matching PROPOSED)
        matchingService.proposeMatchingsToAvailableDrivers(saved);

        enrichClientFacingDetails(saved);
        return saved;
    }

    @Override
    @Transactional
    public DemandePreauthResponseDto preAuthorizePayment(Long demandeId, BigDecimal holdAmount, String paymentMethodRef) {
        DemandeCourse demande = getDemandeCoursById(demandeId);
        if (demande == null) {
            throw new RuntimeException("Demande de course non trouvée");
        }

        if (demande.getStatut() != DemandeStatus.PENDING) {
            throw new IllegalStateException("Pré-autorisation autorisée uniquement pour une demande PENDING");
        }

        BigDecimal estimatedAmount = demande.getPrixEstime() != null ? demande.getPrixEstime() : BigDecimal.ZERO;
        BigDecimal safeHoldAmount = holdAmount != null ? holdAmount : estimatedAmount;
        if (safeHoldAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Montant de pré-autorisation invalide");
        }

        if (estimatedAmount.compareTo(BigDecimal.ZERO) > 0 && safeHoldAmount.compareTo(estimatedAmount) < 0) {
            throw new IllegalArgumentException("Le montant pré-autorisé doit couvrir au moins le prix estimé");
        }

        safeHoldAmount = safeHoldAmount.setScale(2, RoundingMode.HALF_UP);

        demande.setApprobationClientRequise(Boolean.TRUE);
        demande.setPrixClientAccepte(Boolean.TRUE);
        demande.setPrixPropose(safeHoldAmount);
        demande.setDateModification(LocalDateTime.now());
        demandeCoursRepository.save(demande);

        String authorizationRef = "AUTH_" + demandeId + "_" + System.currentTimeMillis();

        return DemandePreauthResponseDto.builder()
                .demandeId(demandeId)
                .authorized(true)
                .holdAmount(safeHoldAmount)
                .authorizationRef(authorizationRef)
                .message("Pré-autorisation validée")
                .build();
    }

    @Override
    @Transactional
    public DemandePreauthResponseDto preAuthorizePenalty(Long demandeId, BigDecimal penaltyAmount, String paymentMethodRef) {
        DemandeCourse demande = getDemandeCoursById(demandeId);
        if (demande == null) {
            throw new RuntimeException("Demande de course non trouvée");
        }

        if (demande.getStatut() != DemandeStatus.PENDING) {
            throw new IllegalStateException("Pré-autorisation penalty autorisée uniquement pour une demande PENDING");
        }

        BigDecimal estimatedAmount = demande.getPrixEstime() != null ? demande.getPrixEstime() : BigDecimal.ZERO;
        BigDecimal minimumPenalty = estimatedAmount.multiply(PENALTY_PREAUTH_RATIO).setScale(2, RoundingMode.HALF_UP);
        BigDecimal safePenaltyAmount = penaltyAmount != null ? penaltyAmount : minimumPenalty;
        safePenaltyAmount = safePenaltyAmount.setScale(2, RoundingMode.HALF_UP);

        if (safePenaltyAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Montant de pré-autorisation penalty invalide");
        }

        if (safePenaltyAmount.compareTo(minimumPenalty) < 0) {
            throw new IllegalArgumentException("Le montant pré-autorisé doit couvrir au moins 20% du prix estimé");
        }

        demande.setApprobationClientRequise(Boolean.TRUE);
        demande.setPrixClientAccepte(Boolean.TRUE);
        demande.setPrixPropose(safePenaltyAmount);
        demande.setDateModification(LocalDateTime.now());
        demandeCoursRepository.save(demande);

        String authorizationRef = "AUTH_PENALTY_" + demandeId + "_" + System.currentTimeMillis();

        return DemandePreauthResponseDto.builder()
                .demandeId(demandeId)
                .authorized(true)
                .holdAmount(safePenaltyAmount)
                .authorizationRef(authorizationRef)
                .message("Pré-autorisation pénalité (20%) validée")
                .build();
    }

    @Override
    @Transactional
    public DemandeCourse confirmAcceptedByClient(Long demandeId) {
        DemandeCourse demande = getDemandeCoursById(demandeId);
        if (demande == null) {
            throw new RuntimeException("Demande de course non trouvée");
        }

        if (demande.getStatut() != DemandeStatus.ACCEPTED) {
            throw new IllegalStateException("La demande doit etre en statut ACCEPTED pour etre confirmee");
        }

        if (demande.getCourse() == null) {
            throw new IllegalStateException("La course n'est pas encore associee a la demande");
        }

        if (Boolean.FALSE.equals(demande.getApprobationClientRequise())) {
            enrichClientFacingDetails(demande);
            return demande;
        }

        demande.setApprobationClientRequise(Boolean.FALSE);
        demande.setDateModification(LocalDateTime.now());
        demande.setConfirmationClientStatut(ConfirmationClientStatut.CONFIRMED);
        DemandeCourse saved = demandeCoursRepository.save(demande);
        enrichClientFacingDetails(saved);
        return saved;
    }

    @Override
    @Transactional
    public DemandeCourse cancelClientConfirmation(Long demandeId) {
        DemandeCourse demande = getDemandeCoursById(demandeId);
        if (demande == null) {
            throw new RuntimeException("Demande de course non trouvée");
        }

        if (demande.getStatut() != DemandeStatus.ACCEPTED) {
            throw new IllegalStateException("Seule une demande ACCEPTED peut annuler la confirmation chauffeur");
        }

        Course course = demande.getCourse();
        if (course == null) {
            throw new IllegalStateException("Aucune course associée à la demande");
        }

        Matching matching = course.getMatching();
        Long previousChauffeurId = course.getChauffeur() != null ? course.getChauffeur().getIdChauffeur() : null;
        Long previousCourseId = course.getIdCourse();

        if (matching != null) {
            matching.setStatut(MatchingStatut.REJECTED);
            matching.setCourse(null);
            matchingRepository.save(matching);
        }

        courseRepository.delete(course);

        demande.setCourse(null);
        demande.setStatut(DemandeStatus.MATCHING);
        demande.setApprobationClientRequise(Boolean.FALSE);
        demande.setConfirmationClientStatut(ConfirmationClientStatut.CANCELLED);
        demande.setDateModification(LocalDateTime.now());
        DemandeCourse saved = demandeCoursRepository.save(demande);

        if (previousChauffeurId != null) {
            DriverNotificationDTO notif = DriverNotificationDTO.builder()
                    .type("COURSE_CONFIRMATION_CANCELLED")
                    .titre("Confirmation annulée")
                    .message("Le client a annulé cette proposition. Retour au dashboard.")
                    .courseId(previousCourseId)
                    .data(java.util.Map.of(
                            "demandeId", demandeId,
                            "courseId", previousCourseId != null ? previousCourseId : 0
                    ))
                    .build();

            realTimeController.sendNotificationToDriver(previousChauffeurId, notif);
        }

    if (previousCourseId != null) {
        messagingTemplate.convertAndSend(
            "/topic/course/" + previousCourseId + "/status",
            java.util.Map.of(
                "courseId", previousCourseId,
                "statut", "CANCELLED",
                "confirmationCancelled", true,
                "demandeId", demandeId
            )
        );
    }

        // Relancer une nouvelle vague de propositions chauffeur sans annuler toute la demande.
        matchingService.proposeMatchingsToAvailableDrivers(saved);

        enrichClientFacingDetails(saved);
        return saved;
    }

    private void enrichClientFacingDetails(DemandeCourse demande) {
        if (demande == null || demande.getCourse() == null || demande.getCourse().getChauffeur() == null) {
            return;
        }

        tn.hypercloud.entity.transport.Chauffeur chauffeur = demande.getCourse().getChauffeur();
        if (chauffeur.getUtilisateur() == null) {
            return;
        }

        User user = chauffeur.getUtilisateur();
        chauffeur.setUtilisateurId(user.getId());
        chauffeur.setNomAffichage(user.getUsername());
        chauffeur.setEmailAffichage(user.getEmail());
        chauffeur.setPhotoProfil(user.getProfileImage());
    }
}
