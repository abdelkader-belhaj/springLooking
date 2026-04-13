package tn.hypercloud.repository.transport;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import tn.hypercloud.entity.transport.Chauffeur;
import tn.hypercloud.entity.transport.Course;
import tn.hypercloud.entity.transport.enums.CourseStatus;

import java.time.LocalDateTime;
import java.util.List;

public interface CourseRepository extends JpaRepository<Course, Long> {
    List<Course> findByChauffeur(Chauffeur chauffeur);
    List<Course> findByStatut(CourseStatus statut);
    List<Course> findByDateCreationBetween(LocalDateTime start, LocalDateTime end);
    List<Course> findByChauffeur_IdChauffeur(Long idChauffeur);
    List<Course> findByDemande_Client_Id(Long clientId);

    @Query("""
        select c
        from Course c
        left join fetch c.demande d
        left join fetch d.client
        left join fetch c.localisationDepart
        left join fetch c.localisationArrivee
        left join fetch c.paiementTransport
        where c.chauffeur.idChauffeur = :chauffeurId
        order by c.dateModification desc
        """)
    List<Course> findHistoryByChauffeurId(@Param("chauffeurId") Long chauffeurId);
}
