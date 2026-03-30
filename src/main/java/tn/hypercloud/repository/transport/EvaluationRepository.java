package tn.hypercloud.repository.transport;

import org.springframework.data.jpa.repository.JpaRepository;
import tn.hypercloud.entity.transport.Course;
import tn.hypercloud.entity.transport.Evaluation;
import tn.hypercloud.entity.transport.enums.EvaluationType;
import tn.hypercloud.entity.user.User;

import java.util.List;

public interface EvaluationRepository extends JpaRepository<Evaluation, Long> {
    List<Evaluation> findByCourse(Course course);
    List<Evaluation> findByEvalue(User evalue);
    List<Evaluation> findByType(EvaluationType type);
}