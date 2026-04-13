package tn.hypercloud.repository.transport;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import tn.hypercloud.entity.transport.DemandeCourse;
import tn.hypercloud.entity.transport.enums.DemandeStatus;
import tn.hypercloud.entity.user.User;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface DemandeCoursRepository extends JpaRepository<DemandeCourse, Long> {
    List<DemandeCourse> findByClient(User client);
    List<DemandeCourse> findByStatut(DemandeStatus statut);
    List<DemandeCourse> findByDateCreationBetween(LocalDateTime start, LocalDateTime end);

    @Query("""
        select d
        from DemandeCourse d
        left join fetch d.client
        left join fetch d.localisationDepart
        left join fetch d.localisationArrivee
        left join fetch d.course c
        left join fetch c.chauffeur ch
        left join fetch ch.utilisateur
        left join fetch c.vehicule
        where d.idDemande = :id
        """)
    Optional<DemandeCourse> findDetailedById(@Param("id") Long id);
}
