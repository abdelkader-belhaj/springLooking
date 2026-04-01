package tn.hypercloud.repository.transport;

import org.springframework.data.jpa.repository.JpaRepository;
import tn.hypercloud.entity.transport.Course;
import tn.hypercloud.entity.transport.EvaluationTransport;
import tn.hypercloud.entity.transport.enums.EvaluationType;
import tn.hypercloud.entity.user.User;

import java.util.List;

public interface EvaluationRepository extends JpaRepository<EvaluationTransport, Long> {
    List<EvaluationTransport> findByCourse(Course course);
    List<EvaluationTransport> findByEvalue(User evalue);
    List<EvaluationTransport> findByType(EvaluationType type);
}