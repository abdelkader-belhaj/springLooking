package tn.hypercloud.repository.reservation;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import tn.hypercloud.entity.reservation.ReservationVol;
import java.util.List;
import java.util.Optional;

public interface ReservationVolRepository extends JpaRepository<ReservationVol, Integer> {

    List<ReservationVol> findByTouristeId(Long touristeId);
    long countByTouristeId(Long touristeId);

    List<ReservationVol> findByStatutReservation(
            ReservationVol.StatutReservation statutReservation
    );
    Optional<ReservationVol> findByReference(String reference);

    @Query("SELECT COUNT(r) FROM ReservationVol r WHERE r.touriste.id = :touristeId AND r.paiement.statut = tn.hypercloud.entity.reservation.PaiementVol.StatutPaiement.paye")
    long countPaidByTouristeId(@Param("touristeId") Long touristeId);

    @Query("SELECT r FROM ReservationVol r WHERE r.volAller = :vol OR r.volRetour = :vol")
    List<ReservationVol> findByVol(@Param("vol") tn.hypercloud.entity.reservation.Vol vol);
}