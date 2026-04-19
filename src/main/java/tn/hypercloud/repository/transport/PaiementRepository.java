package tn.hypercloud.repository.transport;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tn.hypercloud.entity.transport.Paiement;

@Repository
public interface PaiementRepository extends JpaRepository<Paiement, Long> {
}
