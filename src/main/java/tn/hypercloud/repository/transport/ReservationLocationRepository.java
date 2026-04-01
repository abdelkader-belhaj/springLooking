package tn.hypercloud.repository.transport;

import tn.hypercloud.entity.transport.ReservationLocation;
import tn.hypercloud.entity.transport.enums.ReservationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDateTime;
import java.util.List;

public interface ReservationLocationRepository extends JpaRepository<ReservationLocation, Long> {

    List<ReservationLocation> findByClient_Id(Long clientId);

    // Nouvelle méthode pour vérifier disponibilité d'un véhicule d'agence
    boolean existsByVehiculeAgence_IdVehiculeAgenceAndStatutInAndDateDebutLessThanEqualAndDateFinGreaterThanEqual(
            Long vehiculeAgenceId,
            List<ReservationStatus> statuts,
            LocalDateTime dateDebut,
            LocalDateTime dateFin);
}