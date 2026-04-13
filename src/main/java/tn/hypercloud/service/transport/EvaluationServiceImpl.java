package tn.hypercloud.service.transport;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import tn.hypercloud.entity.transport.enums.EvaluationType;
import tn.hypercloud.entity.transport.Course;
import java.util.stream.Collectors;
import tn.hypercloud.entity.transport.EvaluationTransport;
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
    public EvaluationTransport addEvaluation(EvaluationTransport evaluationTransport) {
        // 1. Fetch the full Course
        if (evaluationTransport.getCourse() != null && evaluationTransport.getCourse().getIdCourse() != null) {
            Course course = courseRepository.findById(evaluationTransport.getCourse().getIdCourse())
                    .orElseThrow(() -> new RuntimeException("Course not found"));
            evaluationTransport.setCourse(course);
        } else {
            throw new IllegalArgumentException("Course ID is required");
        }

        // 2. Fetch the evaluateur User using the transient ID
        if (evaluationTransport.getEvaluateurId() != null) {
            User evaluateur = userRepository.findById(evaluationTransport.getEvaluateurId())
                    .orElseThrow(() -> new RuntimeException("Evaluateur not found with id: " + evaluationTransport.getEvaluateurId()));
            evaluationTransport.setEvaluateur(evaluateur);
        } else {
            throw new IllegalArgumentException("Evaluateur ID is required");
        }

        // 3. Fetch the evalue User using the transient ID
        if (evaluationTransport.getEvalueId() != null) {
            User evalue = userRepository.findById(evaluationTransport.getEvalueId())
                    .orElseThrow(() -> new RuntimeException("Evalue not found with id: " + evaluationTransport.getEvalueId()));
            evaluationTransport.setEvalue(evalue);
        } else {
            throw new IllegalArgumentException("Evalue ID is required");
        }

        EvaluationTransport existing = evaluationRepository.findFirstByCourseAndType(
                evaluationTransport.getCourse(),
                evaluationTransport.getType()
        );

        if (existing != null) {
            return enrich(existing);
        }

        return enrich(evaluationRepository.save(evaluationTransport));
    }
    @Override
    public EvaluationTransport updateEvaluation(EvaluationTransport evaluationTransport) {
        return enrich(evaluationRepository.save(evaluationTransport));
    }

    @Override
    public void deleteEvaluation(Long id) {
        evaluationRepository.deleteById(id);
    }

    @Override
    public EvaluationTransport getEvaluationById(Long id) {
        return evaluationRepository.findById(id).map(this::enrich).orElse(null);
    }

    @Override
    public List<EvaluationTransport> getAllEvaluations() {
        return evaluationRepository.findAll().stream().map(this::enrich).collect(Collectors.toList());
    }

    @Override
    public List<EvaluationTransport> getEvaluationsByCourse(Course course) {
        return evaluationRepository.findByCourse(course).stream().map(this::enrich).collect(Collectors.toList());
    }

    @Override
    public List<EvaluationTransport> getEvaluationsForUser(User user) {
        return evaluationRepository.findByEvalue(user).stream().map(this::enrich).collect(Collectors.toList());
    }

    private EvaluationTransport enrich(EvaluationTransport evaluation) {
        if (evaluation == null) {
            return null;
        }

        evaluation.setCourseId(evaluation.getCourse() != null ? evaluation.getCourse().getIdCourse() : null);
        evaluation.setEvaluateurId(evaluation.getEvaluateur() != null ? evaluation.getEvaluateur().getId() : null);
        evaluation.setEvalueId(evaluation.getEvalue() != null ? evaluation.getEvalue().getId() : null);
        evaluation.setEvaluateurNom(evaluation.getEvaluateur() != null ? evaluation.getEvaluateur().getUsername() : null);
        evaluation.setEvalueNom(evaluation.getEvalue() != null ? evaluation.getEvalue().getUsername() : null);
        return evaluation;
    }
}