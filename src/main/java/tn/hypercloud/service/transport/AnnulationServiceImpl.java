package tn.hypercloud.service.transport;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.hypercloud.controller.transport.RealTimeTransportController;
import tn.hypercloud.dto.transport.DriverNotificationDTO;
import tn.hypercloud.entity.transport.AnnulationTransport;
import tn.hypercloud.entity.transport.Chauffeur;
import tn.hypercloud.entity.transport.Course;
import tn.hypercloud.entity.transport.PaiementTransport;
import tn.hypercloud.entity.transport.WalletTransaction;
import tn.hypercloud.entity.transport.enums.AnnulePar;
import tn.hypercloud.entity.transport.enums.CourseStatus;
import tn.hypercloud.entity.transport.enums.PaiementMethode;
import tn.hypercloud.entity.transport.enums.PaiementStatut;
import tn.hypercloud.entity.transport.enums.TransactionType;
import tn.hypercloud.repository.transport.AnnulationRepository;
import tn.hypercloud.repository.transport.ChauffeurRepository;
import tn.hypercloud.repository.transport.CourseRepository;
import tn.hypercloud.repository.transport.WalletTransactionRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@AllArgsConstructor
public class AnnulationServiceImpl implements IAnnulationService {

    private final AnnulationRepository annulationRepository;
    private final CourseRepository courseRepository;
    private final ChauffeurRepository chauffeurRepository;
    private final WalletTransactionRepository walletTransactionRepository;
    private final IPaiementService paiementService;
    private final RealTimeTransportController realTimeController;

    private static final int MINUTES_GRATUITES = 2;           // configurable
    private static final BigDecimal POURCENTAGE_PENALITE = new BigDecimal("0.20"); // 20%

    @Override
    @Transactional
    public AnnulationTransport annulerCourse(Long courseId, AnnulePar annulePar, String raison) {
        if (annulePar != AnnulePar.CLIENT) {
            throw new IllegalStateException("Seule l'annulation CLIENT est autorisée dans ce workflow");
        }


        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new RuntimeException("Course non trouvée"));

        if (annulationRepository.findByCourse(course).isPresent()) {
            throw new IllegalStateException("Cette course est déjà annulée");
        }

        // Règle métier: annulation autorisée uniquement pendant ACCEPTED.
        if (course.getStatut() != CourseStatus.ACCEPTED) {
            throw new IllegalStateException("Annulation autorisée uniquement pour une course ACCEPTED");
        }

        // ====================== CALCUL PÉNALITÉ ======================
        BigDecimal penalite = BigDecimal.ZERO;
        BigDecimal montantBase = course.getDemande() != null && course.getDemande().getPrixEstime() != null
                ? course.getDemande().getPrixEstime()
                : BigDecimal.ZERO;
        BigDecimal montantPreautorise = course.getDemande() != null && course.getDemande().getPrixPropose() != null
            ? course.getDemande().getPrixPropose()
            : BigDecimal.ZERO;

        long minutesDepuisCreation = ChronoUnit.MINUTES.between(course.getDateCreation(), LocalDateTime.now());

        // Pénalité uniquement si le CLIENT annule après la fenêtre gratuite.
        if (minutesDepuisCreation > MINUTES_GRATUITES) {
            penalite = montantBase.multiply(POURCENTAGE_PENALITE)
                    .setScale(2, java.math.RoundingMode.HALF_UP);

            if (!Boolean.TRUE.equals(course.getDemande().getPrixClientAccepte())
                    || montantPreautorise.compareTo(penalite) < 0) {
                throw new IllegalStateException("Pré-autorisation insuffisante pour couvrir la pénalité d'annulation");
            }
        }

        BigDecimal remboursementFinal = montantBase.subtract(penalite);
        if (remboursementFinal.compareTo(BigDecimal.ZERO) < 0) {
            remboursementFinal = BigDecimal.ZERO;
        }
        remboursementFinal = remboursementFinal.setScale(2, RoundingMode.HALF_UP);

        // ====================== CRÉATION DE L'ANNULATION ======================
        AnnulationTransport annulationTransport = new AnnulationTransport();   // ← sans builder

        annulationTransport.setCourse(course);
        annulationTransport.setAnnulePar(annulePar);
        annulationTransport.setRaison(raison != null ? raison : "Annulation par " + annulePar.name());
        annulationTransport.setMontantPenalite(penalite);
        annulationTransport.setMontantRemboursement(remboursementFinal);

        AnnulationTransport saved = annulationRepository.save(annulationTransport);

        // ====================== MISE À JOUR DE LA COURSE ======================
        course.setStatut(CourseStatus.CANCELLED);
        course.setPaymentClientConfirmed(false);
        course.setPaymentVerifiedByDriver(false);
        course.setPaymentVerificationCode(null);
        course.setPaymentIntentId(null);
        course.setPaymentClientConfirmedAt(null);
        course.setPaymentVerifiedAt(null);
        if (course.getDemande() != null) {
            course.getDemande().setPrixClientAccepte(Boolean.FALSE);
            course.getDemande().setApprobationClientRequise(Boolean.FALSE);
        }
        courseRepository.save(course);

        // ====================== GESTION PAIEMENT ANNULATION ======================
        PaiementTransport paiement = paiementService.getPaiementByCourse(course);
        if (paiement != null) {
            paiement.setStatut(PaiementStatut.REFUNDED);
            paiementService.updatePaiement(paiement);
        } else if (penalite.compareTo(BigDecimal.ZERO) > 0) {
            PaiementTransport penaltyPayment = paiementService.createCancellationPenaltyPayment(
                    course,
                    penalite,
                    PaiementMethode.CARD
            );

                Chauffeur chauffeur = course.getChauffeur();
                if (chauffeur != null) {
                BigDecimal net = penaltyPayment.getMontantNet() != null
                    ? penaltyPayment.getMontantNet()
                    : penalite;
                BigDecimal currentSolde = chauffeur.getSolde() != null ? chauffeur.getSolde() : BigDecimal.ZERO;
                chauffeur.setSolde(currentSolde.add(net));
                chauffeurRepository.save(chauffeur);

                walletTransactionRepository.save(WalletTransaction.builder()
                    .chauffeur(chauffeur)
                    .montant(net)
                    .type(TransactionType.CREDIT_COURSE)
                    .description("Pénalité annulation course #" + course.getIdCourse())
                    .paiementTransport(penaltyPayment)
                    .build());
                }

            course.setPaiementTransport(penaltyPayment);
            course.setMontantCommission(penaltyPayment.getMontantCommission());
            courseRepository.save(course);
        }

            if (course.getChauffeur() != null && course.getChauffeur().getIdChauffeur() != null) {
                DriverNotificationDTO notif = DriverNotificationDTO.builder()
                    .type("COURSE_CANCELLED")
                    .titre("Course annulée")
                    .message(penalite.compareTo(BigDecimal.ZERO) > 0
                        ? "Annulation client avec compensation" : "Annulation client sans pénalité")
                    .courseId(course.getIdCourse())
                    .data(java.util.Map.of(
                        "penalite", penalite,
                        "remboursement", remboursementFinal,
                        "annulePar", annulePar.name()
                    ))
                    .build();

                realTimeController.sendNotificationToDriver(course.getChauffeur().getIdChauffeur(), notif);
            }

        return saved;
    }

    // ====================== CRUD DE BASE ======================
    @Override
    public AnnulationTransport addAnnulation(AnnulationTransport annulationTransport) {
        return annulationRepository.save(annulationTransport);
    }

    @Override
    public AnnulationTransport updateAnnulation(AnnulationTransport annulationTransport) {
        return annulationRepository.save(annulationTransport);
    }

    @Override
    public void deleteAnnulation(Long id) {
        annulationRepository.deleteById(id);
    }

    @Override
    public AnnulationTransport getAnnulationById(Long id) {
        return annulationRepository.findById(id).orElse(null);
    }

    @Override
    public List<AnnulationTransport> getAllAnnulations() {
        return annulationRepository.findAll();
    }

    @Override
    public List<AnnulationTransport> getAnnulationsByType(AnnulePar annulePar) {
        return annulationRepository.findByAnnulePar(annulePar);
    }
}
