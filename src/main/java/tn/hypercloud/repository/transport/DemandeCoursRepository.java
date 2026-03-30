package tn.hypercloud.repository.transport;

import org.springframework.data.jpa.repository.JpaRepository;
import tn.hypercloud.entity.transport.DemandeCourse;
import tn.hypercloud.entity.transport.enums.DemandeStatus;
import tn.hypercloud.entity.user.User;

import java.time.LocalDateTime;
import java.util.List;

public interface DemandeCoursRepository extends JpaRepository<DemandeCourse, Long> {
    List<DemandeCourse> findByClient(User client);
    List<DemandeCourse> findByStatut(DemandeStatus statut);
    List<DemandeCourse> findByDateCreationBetween(LocalDateTime start, LocalDateTime end);
}
