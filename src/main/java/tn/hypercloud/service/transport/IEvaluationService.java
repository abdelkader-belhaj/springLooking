package tn.hypercloud.service.transport;

import tn.hypercloud.entity.transport.Course;
import tn.hypercloud.entity.transport.Evaluation;
import tn.hypercloud.entity.user.User;

import java.util.List;
public interface IEvaluationService {
    Evaluation addEvaluation(Evaluation evaluation);
    Evaluation updateEvaluation(Evaluation evaluation);
    void deleteEvaluation(Long id);
    Evaluation getEvaluationById(Long id);
    List<Evaluation> getAllEvaluations();
    List<Evaluation> getEvaluationsByCourse(Course course);
    List<Evaluation> getEvaluationsForUser(User user);
}
