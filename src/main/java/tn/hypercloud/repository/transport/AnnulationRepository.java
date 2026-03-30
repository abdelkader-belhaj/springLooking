package tn.hypercloud.repository.transport;

import org.springframework.data.jpa.repository.JpaRepository;
import tn.hypercloud.entity.transport.Annulation;
import tn.hypercloud.entity.transport.Course;
import tn.hypercloud.entity.transport.enums.AnnulePar;

import java.util.List;
import java.util.Optional;
public interface AnnulationRepository extends JpaRepository<Annulation, Long> {
    Optional<Annulation> findByCourse(Course course);
    List<Annulation> findByAnnulePar(AnnulePar annulePar);
}