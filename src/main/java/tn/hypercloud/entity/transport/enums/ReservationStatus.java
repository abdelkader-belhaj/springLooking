package tn.hypercloud.entity.transport.enums;

public enum ReservationStatus {
    PENDING,
    DRAFT,                  // Brouillon
    KYC_PENDING,            // Attente vérif permis
    DEPOSIT_HELD,           // Caution bloquée
    CONTRACT_SIGNED,        // Contrat signé
    CONFIRMED,
    IN_PROGRESS,
    ACTIVE,
    COMPLETED, //terminé
    CANCELLED //annulé

}