package tn.hypercloud.service.transport;

import tn.hypercloud.dto.transport.DriverReviewSummaryDto;
import tn.hypercloud.entity.transport.Course;
import tn.hypercloud.entity.transport.EvaluationTransport;
import tn.hypercloud.entity.user.User;

import java.util.List;
public interface IEvaluationService {
    EvaluationTransport addEvaluation(EvaluationTransport evaluationTransport);
    EvaluationTransport updateEvaluation(EvaluationTransport evaluationTransport);
    void deleteEvaluation(Long id);
    EvaluationTransport getEvaluationById(Long id);
    List<EvaluationTransport> getAllEvaluations();
    List<EvaluationTransport> getEvaluationsByCourse(Course course);
    List<EvaluationTransport> getEvaluationsForUser(User user);
    List<EvaluationTransport> getClientReviewsForChauffeur(Long chauffeurId);
    DriverReviewSummaryDto getDriverReviewSummary(Long chauffeurId);
}
