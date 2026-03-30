package tn.hypercloud.repository.transport;

import org.springframework.data.jpa.repository.JpaRepository;
import tn.hypercloud.entity.transport.Chauffeur;
import tn.hypercloud.entity.transport.DemandeCourse;
import tn.hypercloud.entity.transport.Matching;
import tn.hypercloud.entity.transport.enums.MatchingStatut;

import java.util.List;
import java.util.Optional;
public interface MatchingRepository extends JpaRepository<Matching, Long> {
    Optional<Matching> findByDemande(DemandeCourse demande);
    List<Matching> findAllByDemande(DemandeCourse demande);
    List<Matching> findByChauffeur(Chauffeur chauffeur);
    List<Matching> findByStatut(MatchingStatut statut);
}
