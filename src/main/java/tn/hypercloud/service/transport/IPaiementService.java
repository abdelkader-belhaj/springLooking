package tn.hypercloud.service.transport;

import tn.hypercloud.entity.transport.PaiementTransport;
import tn.hypercloud.entity.transport.Course;
import tn.hypercloud.entity.transport.enums.PaiementMethode;
import tn.hypercloud.entity.transport.enums.PaiementStatut;

import java.math.BigDecimal;
import java.util.List;
public interface IPaiementService {
    PaiementTransport addPaiement(PaiementTransport paiementTransport);
    PaiementTransport updatePaiement(PaiementTransport paiementTransport);
    void deletePaiement(Long id);
    PaiementTransport getPaiementById(Long id);
    List<PaiementTransport> getAllPaiements();
    List<PaiementTransport> getPaiementsByStatut(PaiementStatut statut);
    PaiementTransport completePaiement(Long id);
    PaiementTransport refundPaiement(Long id);

    PaiementTransport getPaiementByCourse(Course course);

    PaiementTransport createCompletedCoursePayment(Course course, PaiementMethode methode);

    PaiementTransport createCancellationPenaltyPayment(Course course, BigDecimal penaltyAmount, PaiementMethode methode);

    BigDecimal getPlateformeSolde();
}
