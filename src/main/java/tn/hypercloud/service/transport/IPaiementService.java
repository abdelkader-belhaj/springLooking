package tn.hypercloud.service.transport;

import tn.hypercloud.entity.transport.Paiement;
import tn.hypercloud.entity.transport.enums.PaiementStatut;

import java.util.List;
public interface IPaiementService {
    Paiement addPaiement(Paiement paiement);
    Paiement updatePaiement(Paiement paiement);
    void deletePaiement(Long id);
    Paiement getPaiementById(Long id);
    List<Paiement> getAllPaiements();
    List<Paiement> getPaiementsByStatut(PaiementStatut statut);
    Paiement completePaiement(Long id);
    Paiement refundPaiement(Long id);
}
