package tn.hypercloud.service.transport;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tn.hypercloud.entity.transport.WalletTransaction;
import tn.hypercloud.entity.transport.Chauffeur;
import tn.hypercloud.entity.transport.AgenceLocation;
import tn.hypercloud.repository.transport.WalletTransactionRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class WalletTransactionServiceImpl implements IWalletTransactionService {

    private final WalletTransactionRepository walletTransactionRepository;

    @Override
    public List<WalletTransaction> getTransactionsByChauffeur(Chauffeur chauffeur) {
        return walletTransactionRepository.findByChauffeurOrderByDateTransactionDesc(chauffeur);
    }

    @Override
    public List<WalletTransaction> getTransactionsByAgence(AgenceLocation agence) {
        return walletTransactionRepository.findByAgenceOrderByDateTransactionDesc(agence);
    }

    @Override
    public List<WalletTransaction> getRecentTransactions(int limit) {
        return walletTransactionRepository.findTop50ByOrderByDateTransactionDesc()
                .stream()
                .limit(limit)
                .toList();
    }
}