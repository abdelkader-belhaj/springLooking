package tn.hypercloud.entity.transport.enums;

public enum TransactionType {
    CREDIT_COURSE,          // montantNet vers chauffeur
    CREDIT_RESERVATION,     // montantNet vers agence
    CREDIT_COMMISSION,      // commission vers plateforme
    DEBIT_PAYOUT            // futur : virement sortant
}