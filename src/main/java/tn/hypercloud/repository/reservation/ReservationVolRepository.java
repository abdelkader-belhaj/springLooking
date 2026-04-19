package tn.hypercloud.repository.reservation;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tn.hypercloud.entity.reservation.ReservationVol;

@Repository
public interface ReservationVolRepository extends JpaRepository<ReservationVol, Integer> {
}
