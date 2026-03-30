package tn.hypercloud.repository.transport;

import org.springframework.data.jpa.repository.JpaRepository;
import tn.hypercloud.entity.transport.Chauffeur;
import tn.hypercloud.entity.transport.Vehicule;
import tn.hypercloud.entity.transport.enums.TypeVehicule;
import tn.hypercloud.entity.transport.enums.VehiculeStatut;

import java.util.List;
import java.util.Optional;
public interface VehiculeRepository extends JpaRepository<Vehicule, Long> {
    Optional<Vehicule> findByNumeroPlaque(String numeroPlaque);
    List<Vehicule> findByChauffeur(Chauffeur chauffeur);
    List<Vehicule> findByTypeVehicule(TypeVehicule typeVehicule);
    List<Vehicule> findByStatut(VehiculeStatut statut);
    boolean existsByNumeroPlaque(String numeroPlaque);
}
