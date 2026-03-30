package tn.hypercloud.service.transport;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.hypercloud.entity.transport.Annulation;
import tn.hypercloud.entity.transport.Course;
import tn.hypercloud.entity.transport.enums.AnnulePar;
import tn.hypercloud.entity.transport.enums.CourseStatus;
import tn.hypercloud.entity.transport.enums.PaiementStatut;
import tn.hypercloud.repository.transport.AnnulationRepository;
import tn.hypercloud.repository.transport.CourseRepository;
import tn.hypercloud.repository.transport.PaiementRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@AllArgsConstructor
public class AnnulationServiceImpl implements IAnnulationService {

    private final AnnulationRepository annulationRepository;
    private final CourseRepository courseRepository;
    private final PaiementRepository paiementRepository;

    private static final int MINUTES_GRATUITES = 5;           // configurable
    private static final BigDecimal POURCENTAGE_PENALITE = new BigDecimal("0.20"); // 20%

    @Override
    @Transactional
    public Annulation annulerCourse(Long courseId, AnnulePar annulePar, String raison) {

        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new RuntimeException("Course non trouvée"));

        // Impossible d'annuler une course déjà terminée
        if (course.getStatut() == CourseStatus.COMPLETED) {
            throw new IllegalStateException("Impossible d'annuler une course terminée");
        }

        // ====================== CALCUL PÉNALITÉ ======================
        BigDecimal penalite = BigDecimal.ZERO;
        BigDecimal montantRemboursement = course.getPrixFinal() != null
                ? course.getPrixFinal()
                : course.getDemande().getPrixEstime();

        long minutesDepuisCreation = ChronoUnit.MINUTES.between(course.getDateCreation(), LocalDateTime.now());

        // Annulation gratuite avant X minutes (comme dans le PDF)
        if (minutesDepuisCreation > MINUTES_GRATUITES) {
            penalite = montantRemboursement.multiply(POURCENTAGE_PENALITE)
                    .setScale(2, java.math.RoundingMode.HALF_UP);
        }

        // Si le chauffeur était déjà en route → compensation supplémentaire
        if (course.getStatut() == CourseStatus.STARTED || course.getStatut() == CourseStatus.IN_PROGRESS) {
            penalite = penalite.add(new BigDecimal("8.00")); // 8 TND de compensation chauffeur
        }

        BigDecimal remboursementFinal = montantRemboursement.subtract(penalite);

        // ====================== CRÉATION DE L'ANNULATION ======================
        Annulation annulation = new Annulation();   // ← sans builder

        annulation.setCourse(course);
        annulation.setAnnulePar(annulePar);
        annulation.setRaison(raison != null ? raison : "Annulation par " + annulePar.name());
        annulation.setMontantPenalite(penalite);
        annulation.setMontantRemboursement(remboursementFinal);

        Annulation saved = annulationRepository.save(annulation);

        // ====================== MISE À JOUR DE LA COURSE ======================
        course.setStatut(CourseStatus.CANCELLED);
        courseRepository.save(course);

        // ====================== REMBOURSEMENT AUTOMATIQUE ======================
        paiementRepository.findByCourse(course).ifPresent(paiement -> {
            paiement.setStatut(PaiementStatut.REFUNDED);
            paiementRepository.save(paiement);
        });

        return saved;
    }

    // ====================== CRUD DE BASE ======================
    @Override
    public Annulation addAnnulation(Annulation annulation) {
        return annulationRepository.save(annulation);
    }

    @Override
    public Annulation updateAnnulation(Annulation annulation) {
        return annulationRepository.save(annulation);
    }

    @Override
    public void deleteAnnulation(Long id) {
        annulationRepository.deleteById(id);
    }

    @Override
    public Annulation getAnnulationById(Long id) {
        return annulationRepository.findById(id).orElse(null);
    }

    @Override
    public List<Annulation> getAllAnnulations() {
        return annulationRepository.findAll();
    }

    @Override
    public List<Annulation> getAnnulationsByType(AnnulePar annulePar) {
        return annulationRepository.findByAnnulePar(annulePar);
    }
}
