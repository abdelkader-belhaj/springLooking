package tn.hypercloud.controller.transport;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.hypercloud.entity.transport.WalletTransaction;
import tn.hypercloud.entity.transport.Chauffeur;
import tn.hypercloud.entity.transport.AgenceLocation;
import tn.hypercloud.service.transport.IWalletTransactionService;
import tn.hypercloud.repository.transport.ChauffeurRepository;
import tn.hypercloud.repository.transport.AgenceLocationRepository;

import java.util.List;

@RestController
@RequestMapping("/api/wallet")
@RequiredArgsConstructor
public class WalletTransactionController {

    private final IWalletTransactionService walletTransactionService;
    private final ChauffeurRepository chauffeurRepository;
    private final AgenceLocationRepository agenceLocationRepository;

    // Historique d'un chauffeur (ex: /api/wallet/chauffeur/5/transactions)
    @GetMapping("/chauffeur/{chauffeurId}/transactions")
    public ResponseEntity<List<WalletTransaction>> getChauffeurTransactions(@PathVariable Long chauffeurId) {
        Chauffeur chauffeur = chauffeurRepository.findById(chauffeurId)
                .orElseThrow(() -> new RuntimeException("Chauffeur non trouvé"));
        List<WalletTransaction> transactions = walletTransactionService.getTransactionsByChauffeur(chauffeur);
        return ResponseEntity.ok(transactions);
    }

    // Historique d'une agence
    @GetMapping("/agence/{agenceId}/transactions")
    public ResponseEntity<List<WalletTransaction>> getAgenceTransactions(@PathVariable Long agenceId) {
        AgenceLocation agence = agenceLocationRepository.findById(agenceId)
                .orElseThrow(() -> new RuntimeException("Agence non trouvée"));
        List<WalletTransaction> transactions = walletTransactionService.getTransactionsByAgence(agence);
        return ResponseEntity.ok(transactions);
    }

    // Dernières transactions (pour dashboard admin)
    @GetMapping("/recent")
    public ResponseEntity<List<WalletTransaction>> getRecentTransactions(@RequestParam(defaultValue = "20") int limit) {
        return ResponseEntity.ok(walletTransactionService.getRecentTransactions(limit));
    }
}