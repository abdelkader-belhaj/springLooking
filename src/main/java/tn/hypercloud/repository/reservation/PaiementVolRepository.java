package tn.hypercloud.repository.reservation;

import org.springframework.data.jpa.repository.JpaRepository;
import tn.hypercloud.entity.reservation.PaiementVol;

import java.util.Optional;

public interface PaiementVolRepository extends JpaRepository<PaiementVol, Integer> {

    // ← AJOUT pour le webhook Stripe
    Optional<PaiementVol> findByReferenceTx(String referenceTx);
}