package tn.hypercloud.repository.transport;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import tn.hypercloud.entity.transport.Course;
import tn.hypercloud.entity.transport.EvaluationTransport;
import tn.hypercloud.entity.transport.enums.EvaluationType;
import tn.hypercloud.entity.user.User;

import java.util.List;

public interface EvaluationRepository extends JpaRepository<EvaluationTransport, Long> {
    @EntityGraph(attributePaths = {"course", "evaluateur", "evalue"})
    List<EvaluationTransport> findAll();

    @EntityGraph(attributePaths = {"course", "evaluateur", "evalue"})
    List<EvaluationTransport> findByCourse(Course course);

    @EntityGraph(attributePaths = {"course", "evaluateur", "evalue"})
    List<EvaluationTransport> findByEvalue(User evalue);

    @EntityGraph(attributePaths = {"course", "evaluateur", "evalue"})
    List<EvaluationTransport> findByType(EvaluationType type);

    @EntityGraph(attributePaths = {"course", "evaluateur", "evalue"})
    EvaluationTransport findFirstByCourseAndType(Course course, EvaluationType type);

    @EntityGraph(attributePaths = {"course", "course.chauffeur", "evaluateur", "evalue"})
    List<EvaluationTransport> findByTypeAndCourse_Chauffeur_IdChauffeur(
            EvaluationType type,
            Long chauffeurId
    );
}