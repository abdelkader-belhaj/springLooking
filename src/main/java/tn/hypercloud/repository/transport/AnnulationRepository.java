package tn.hypercloud.repository.transport;

import org.springframework.data.jpa.repository.JpaRepository;
import tn.hypercloud.entity.transport.AnnulationTransport;
import tn.hypercloud.entity.transport.Course;
import tn.hypercloud.entity.transport.enums.AnnulePar;

import java.util.List;
import java.util.Optional;
public interface AnnulationRepository extends JpaRepository<AnnulationTransport, Long> {
    Optional<AnnulationTransport> findByCourse(Course course);
    List<AnnulationTransport> findByAnnulePar(AnnulePar annulePar);
}