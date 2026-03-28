package tn.hypercloud.repository.reservation;

import org.springframework.data.jpa.repository.JpaRepository;
import tn.hypercloud.entity.reservation.ReservationVol;
import java.util.List;

public interface ReservationVolRepository extends JpaRepository<ReservationVol, Integer> {
    List<ReservationVol> findByTouristeId(Long touristeId);
}