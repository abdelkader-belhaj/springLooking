package tn.hypercloud.repository.transport;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tn.hypercloud.entity.transport.Chauffeur;

@Repository
public interface ChauffeurRepository extends JpaRepository<Chauffeur, Long> {
}
