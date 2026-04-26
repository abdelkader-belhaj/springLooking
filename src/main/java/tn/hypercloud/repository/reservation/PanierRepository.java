package tn.hypercloud.repository.reservation;

import org.springframework.data.jpa.repository.JpaRepository;
import tn.hypercloud.entity.reservation.Panier;
import java.util.List;

public interface PanierRepository extends JpaRepository<Panier, Integer> {
    List<Panier> findByTouristeId(Long touristeId);
}