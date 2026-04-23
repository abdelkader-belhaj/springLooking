package tn.hypercloud.repository.transport;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import tn.hypercloud.entity.transport.Chauffeur;
import tn.hypercloud.entity.transport.DemandeCourse;
import tn.hypercloud.entity.transport.Matching;
import tn.hypercloud.entity.transport.enums.DemandeStatus;
import tn.hypercloud.entity.transport.enums.MatchingStatut;

import java.util.List;
import java.util.Optional;
public interface MatchingRepository extends JpaRepository<Matching, Long> {
    Optional<Matching> findByDemande(DemandeCourse demande);
    List<Matching> findAllByDemande(DemandeCourse demande);
    List<Matching> findByChauffeur(Chauffeur chauffeur);
    List<Matching> findByStatut(MatchingStatut statut);
    List<Matching> findByChauffeur_IdChauffeur(Long chauffeurId);
    // MatchingRepository
    @Query("""
select m from Matching m
join fetch m.chauffeur ch
left join fetch m.demande d
left join fetch d.client c
left join fetch d.localisationDepart ld
left join fetch d.localisationArrivee la
where ch.idChauffeur = :chauffeurId
and m.statut = :statut
    and (d is null or d.statut <> :cancelledStatus)
order by m.dateCreation desc
""")
    List<Matching> findDetailedByChauffeurAndStatut(
            @Param("chauffeurId") Long chauffeurId,
            @Param("statut") MatchingStatut statut,
            @Param("cancelledStatus") DemandeStatus cancelledStatus
    );

        @Query("""
    select m from Matching m
    left join fetch m.demande d
    left join fetch m.chauffeur ch
    where ch.idChauffeur = :chauffeurId
    and (d is null or d.statut <> :cancelledStatus)
    order by m.dateCreation desc
    """)
        List<Matching> findByChauffeurIdExcludingCancelled(
            @Param("chauffeurId") Long chauffeurId,
            @Param("cancelledStatus") DemandeStatus cancelledStatus
        );

    @Query("""
select m from Matching m
join fetch m.chauffeur ch
left join fetch m.demande d
left join fetch d.client c
left join fetch d.localisationDepart ld
left join fetch d.localisationArrivee la
where m.idMatching = :id
""")
    Optional<Matching> findDetailedById(@Param("id") Long id);
}
