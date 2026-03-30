package tn.hypercloud.service.transport;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import tn.hypercloud.entity.transport.Course;
import tn.hypercloud.entity.transport.Evaluation;
import tn.hypercloud.entity.user.User;
import tn.hypercloud.repository.transport.CourseRepository;
import tn.hypercloud.repository.transport.EvaluationRepository;
import tn.hypercloud.repository.user.UserRepository;

import java.util.List;
@Service
@AllArgsConstructor
public class EvaluationServiceImpl implements IEvaluationService {
    private final EvaluationRepository evaluationRepository;
    private final CourseRepository courseRepository;
    private final UserRepository userRepository;
    @Override
    public Evaluation addEvaluation(Evaluation evaluation) {
        // 1. Fetch the full Course
        if (evaluation.getCourse() != null && evaluation.getCourse().getIdCourse() != null) {
            Course course = courseRepository.findById(evaluation.getCourse().getIdCourse())
                    .orElseThrow(() -> new RuntimeException("Course not found"));
            evaluation.setCourse(course);
        } else {
            throw new IllegalArgumentException("Course ID is required");
        }

        // 2. Fetch the evaluateur User using the transient ID
        if (evaluation.getEvaluateurId() != null) {
            User evaluateur = userRepository.findById(evaluation.getEvaluateurId())
                    .orElseThrow(() -> new RuntimeException("Evaluateur not found with id: " + evaluation.getEvaluateurId()));
            evaluation.setEvaluateur(evaluateur);
        } else {
            throw new IllegalArgumentException("Evaluateur ID is required");
        }

        // 3. Fetch the evalue User using the transient ID
        if (evaluation.getEvalueId() != null) {
            User evalue = userRepository.findById(evaluation.getEvalueId())
                    .orElseThrow(() -> new RuntimeException("Evalue not found with id: " + evaluation.getEvalueId()));
            evaluation.setEvalue(evalue);
        } else {
            throw new IllegalArgumentException("Evalue ID is required");
        }

        return evaluationRepository.save(evaluation);
    }
    @Override
    public Evaluation updateEvaluation(Evaluation evaluation) {
        return evaluationRepository.save(evaluation);
    }

    @Override
    public void deleteEvaluation(Long id) {
        evaluationRepository.deleteById(id);
    }

    @Override
    public Evaluation getEvaluationById(Long id) {
        return evaluationRepository.findById(id).orElse(null);
    }

    @Override
    public List<Evaluation> getAllEvaluations() {
        return evaluationRepository.findAll();
    }

    @Override
    public List<Evaluation> getEvaluationsByCourse(Course course) {
        return evaluationRepository.findByCourse(course);
    }

    @Override
    public List<Evaluation> getEvaluationsForUser(User user) {
        return evaluationRepository.findByEvalue(user);
    }
}