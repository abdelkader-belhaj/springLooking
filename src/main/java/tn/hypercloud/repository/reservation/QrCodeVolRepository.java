package tn.hypercloud.repository.reservation;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import tn.hypercloud.entity.reservation.QrCodeVol;
import java.util.Optional;

public interface QrCodeVolRepository extends JpaRepository<QrCodeVol, Integer> {

    @Query("SELECT q FROM QrCodeVol q WHERE q.reservation.id = :reservationId")
    Optional<QrCodeVol> findByReservationId(@Param("reservationId") Integer reservationId);
}