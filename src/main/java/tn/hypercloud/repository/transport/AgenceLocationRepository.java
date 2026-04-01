package tn.hypercloud.repository.transport;

import tn.hypercloud.entity.transport.AgenceLocation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AgenceLocationRepository extends JpaRepository<AgenceLocation, Long> {

    List<AgenceLocation> findByStatut(boolean statut);

    boolean existsByUtilisateurId(Long utilisateurId);
}