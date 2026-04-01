package tn.hypercloud.repository.transport;

import org.springframework.data.jpa.repository.JpaRepository;
import tn.hypercloud.entity.transport.VehiculeAgence;
import java.util.List;

public interface VehiculeAgenceRepository extends JpaRepository<VehiculeAgence, Long> {
    List<VehiculeAgence> findByAgence_IdAgence(Long agenceId);
    boolean existsByNumeroPlaque(String numeroPlaque);
}