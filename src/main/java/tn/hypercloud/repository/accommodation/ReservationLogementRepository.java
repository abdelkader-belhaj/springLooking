package tn.hypercloud.repository.accommodation;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tn.hypercloud.entity.accommodation.ReservationLogement;

@Repository
public interface ReservationLogementRepository extends JpaRepository<ReservationLogement, Integer> {
}
