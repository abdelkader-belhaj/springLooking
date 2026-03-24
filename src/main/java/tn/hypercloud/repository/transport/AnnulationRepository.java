package tn.hypercloud.repository.transport;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tn.hypercloud.entity.transport.Annulation;

@Repository
public interface AnnulationRepository extends JpaRepository<Annulation, Long> {
}
