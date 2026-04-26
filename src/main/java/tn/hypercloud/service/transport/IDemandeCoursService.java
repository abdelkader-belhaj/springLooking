package tn.hypercloud.service.transport;

import tn.hypercloud.dto.transport.DemandePreauthResponseDto;
import tn.hypercloud.entity.transport.DemandeCourse;
import tn.hypercloud.entity.transport.enums.DemandeStatus;

import java.math.BigDecimal;
import java.util.List;

public interface IDemandeCoursService {

    // ====================== CRUD ======================
    DemandeCourse addDemandeCourse(DemandeCourse demandeCourse);
    DemandeCourse updateDemandeCourse(DemandeCourse demandeCourse);
    void deleteDemandeCourse(Long id);
    DemandeCourse getDemandeCoursById(Long id);
    List<DemandeCourse> getAllDemandeCourses();
    List<DemandeCourse> getDemandesByStatut(DemandeStatus statut);

    // ====================== WORKFLOW ======================
    DemandeCourse updateStatut(Long id, DemandeStatus statut);

    /**
     * POINT 3 + 4 du PDF : Passage en MATCHING + broadcast automatique
     * Appelé par PUT /hypercloud/demandes-courses/{id}/matching
     */
    DemandeCourse startMatching(Long id);

    DemandePreauthResponseDto preAuthorizePayment(Long demandeId, BigDecimal holdAmount, String paymentMethodRef);

    DemandePreauthResponseDto preAuthorizePenalty(Long demandeId, BigDecimal penaltyAmount, String paymentMethodRef);

    DemandeCourse confirmAcceptedByClient(Long demandeId);

    DemandeCourse cancelClientConfirmation(Long demandeId);
}
