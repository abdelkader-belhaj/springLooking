package tn.hypercloud.service.transport;

import tn.hypercloud.entity.transport.WalletTransaction;
import tn.hypercloud.entity.transport.Chauffeur;
import tn.hypercloud.entity.transport.AgenceLocation;
import java.util.List;

public interface IWalletTransactionService {

    List<WalletTransaction> getTransactionsByChauffeur(Chauffeur chauffeur);
    List<WalletTransaction> getTransactionsByAgence(AgenceLocation agence);
    List<WalletTransaction> getRecentTransactions(int limit);
}