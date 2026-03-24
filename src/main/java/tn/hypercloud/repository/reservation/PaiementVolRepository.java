package tn.hypercloud.repository.reservation;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tn.hypercloud.entity.reservation.PaiementVol;

@Repository
public interface PaiementVolRepository extends JpaRepository<PaiementVol, Integer> {
}
