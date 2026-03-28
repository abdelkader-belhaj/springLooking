package tn.hypercloud.repository.reservation;

import org.springframework.data.jpa.repository.JpaRepository;
import tn.hypercloud.entity.reservation.PaiementVol;

public interface PaiementVolRepository extends JpaRepository<PaiementVol, Integer> {
}