package tn.hypercloud.repository.reservation;

import org.springframework.data.jpa.repository.JpaRepository;
import tn.hypercloud.entity.reservation.ReservationVol;
import java.util.List;

public interface ReservationVolRepository extends JpaRepository<ReservationVol, Integer> {

    // Existant
    List<ReservationVol> findByTouristeId(Long touristeId);

    // ← NOUVEAU : pour récupérer les annulations
    List<ReservationVol> findByStatutReservation(
            ReservationVol.StatutReservation statutReservation
    );
}