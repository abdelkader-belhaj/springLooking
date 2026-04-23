package tn.hypercloud.repository.transport;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tn.hypercloud.entity.transport.WalletTransaction;
import tn.hypercloud.entity.transport.Chauffeur;
import tn.hypercloud.entity.transport.AgenceLocation;

import java.util.List;

@Repository
public interface WalletTransactionRepository extends JpaRepository<WalletTransaction, Long> {

    // Historique par chauffeur (très utile pour le dashboard driver)
    List<WalletTransaction> findByChauffeurOrderByDateTransactionDesc(Chauffeur chauffeur);

    // Historique par agence
    List<WalletTransaction> findByAgenceOrderByDateTransactionDesc(AgenceLocation agence);

    // Optionnel : toutes les transactions récentes (pour admin)
    List<WalletTransaction> findTop50ByOrderByDateTransactionDesc();
}