package tn.hypercloud.repository.reservation;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tn.hypercloud.entity.reservation.Panier;

@Repository
public interface PanierRepository extends JpaRepository<Panier, Integer> {
}
