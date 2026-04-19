package tn.hypercloud.repository.reservation;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tn.hypercloud.entity.reservation.Vol;

@Repository
public interface VolRepository extends JpaRepository<Vol, Integer> {
}
