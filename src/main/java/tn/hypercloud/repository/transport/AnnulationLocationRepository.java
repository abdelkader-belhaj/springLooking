package tn.hypercloud.repository.transport;

import org.springframework.data.jpa.repository.JpaRepository;
import tn.hypercloud.entity.transport.AnnulationLocation;

public interface AnnulationLocationRepository extends JpaRepository<AnnulationLocation, Long> {
}
