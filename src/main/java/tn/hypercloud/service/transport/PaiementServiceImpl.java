package tn.hypercloud.service.transport;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.hypercloud.entity.transport.Course;
import tn.hypercloud.entity.transport.PaiementTransport;
import tn.hypercloud.entity.transport.enums.PaiementMethode;
import tn.hypercloud.entity.transport.enums.PaiementStatut;
import tn.hypercloud.entity.transport.enums.PaiementType;
import tn.hypercloud.repository.transport.PaiementRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
@Service
@AllArgsConstructor
public class PaiementServiceImpl implements IPaiementService {
    private final PaiementRepository paiementRepository;

    @Override
    public PaiementTransport addPaiement(PaiementTransport paiementTransport) {
        enforceCoursePaymentSecurity(paiementTransport);
        return paiementRepository.save(paiementTransport);
    }

    @Override
    public PaiementTransport updatePaiement(PaiementTransport paiementTransport) {
        enforceCoursePaymentSecurity(paiementTransport);
        return paiementRepository.save(paiementTransport);
    }

    @Override
    public void deletePaiement(Long id) {
        paiementRepository.deleteById(id);
    }

    @Override
    public PaiementTransport getPaiementById(Long id) {
        return paiementRepository.findById(id).orElse(null);
    }

    @Override
    public List<PaiementTransport> getAllPaiements() {
        return paiementRepository.findAll();
    }

    @Override
    public List<PaiementTransport> getPaiementsByStatut(PaiementStatut statut) {
        return paiementRepository.findByStatut(statut);
    }

    @Override
    public PaiementTransport completePaiement(Long id) {
        PaiementTransport paiementTransport = getPaiementById(id);
        if (paiementTransport == null) return null;
        paiementTransport.setStatut(PaiementStatut.COMPLETED);
        return paiementRepository.save(paiementTransport);
    }

    @Override
    public PaiementTransport refundPaiement(Long id) {
        PaiementTransport paiementTransport = getPaiementById(id);
        if (paiementTransport == null) return null;
        paiementTransport.setStatut(PaiementStatut.REFUNDED);
        return paiementRepository.save(paiementTransport);
    }

    @Override
    public PaiementTransport getPaiementByCourse(Course course) {
        if (course == null) {
            return null;
        }

        return paiementRepository.findByCourse(course).orElse(null);
    }

    @Override
    @Transactional
    public PaiementTransport createCompletedCoursePayment(Course course, PaiementMethode methode) {
        if (course == null) {
            throw new IllegalArgumentException("Course est obligatoire");
        }

        if (course.getPrixFinal() == null || course.getPrixFinal().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalStateException("Prix final invalide pour la création du paiement");
        }

        PaiementTransport existing = getPaiementByCourse(course);
        if (existing != null) {
            return existing;
        }

        PaiementTransport paiementTransport = PaiementTransport.builder()
                .course(course)
                .montantTotal(course.getPrixFinal())
                .methode(methode != null ? methode : PaiementMethode.CARD)
                .statut(PaiementStatut.COMPLETED)
                .datePaiement(LocalDateTime.now())
                .build();

        return paiementRepository.save(paiementTransport);
    }

    @Override
    @Transactional
    public PaiementTransport createCancellationPenaltyPayment(Course course, BigDecimal penaltyAmount, PaiementMethode methode) {
        if (course == null) {
            throw new IllegalArgumentException("Course est obligatoire");
        }

        if (penaltyAmount == null || penaltyAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Montant de pénalité invalide");
        }

        PaiementTransport existing = getPaiementByCourse(course);
        if (existing != null) {
            return existing;
        }

        PaiementTransport paiementTransport = PaiementTransport.builder()
                .course(course)
            .montantTotal(penaltyAmount.setScale(2, RoundingMode.HALF_UP))
                .methode(methode != null ? methode : PaiementMethode.CARD)
                .statut(PaiementStatut.COMPLETED)
                .datePaiement(LocalDateTime.now())
                .typePaiement(PaiementType.COURSE)
                .build();

        // Cas métier spécial annulation: on enregistre une pénalité sans flow de vérification code.
        return paiementRepository.save(paiementTransport);
    }

    private void enforceCoursePaymentSecurity(PaiementTransport paiementTransport) {
        if (paiementTransport == null || paiementTransport.getCourse() == null) {
            return;
        }

        Course course = paiementTransport.getCourse();

        PaiementType type = paiementTransport.getTypePaiement();
        boolean isCourseType = type == null || type == PaiementType.COURSE;
        if (!isCourseType) {
            return;
        }

        if (!course.isPaymentClientConfirmed() || !course.isPaymentVerifiedByDriver()) {
            throw new IllegalStateException(
                    "Paiement COURSE interdit: validation client + chauffeur requise avant création"
            );
        }
    }

    @Override
    public BigDecimal getPlateformeSolde() {
        return paiementRepository.sumMontantCommissionByStatut(PaiementStatut.COMPLETED);
    }
}