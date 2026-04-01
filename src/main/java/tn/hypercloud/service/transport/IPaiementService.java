package tn.hypercloud.service.transport;

import tn.hypercloud.entity.transport.PaiementTransport;
import tn.hypercloud.entity.transport.enums.PaiementStatut;

import java.math.BigDecimal;
import java.util.List;
public interface IPaiementService {
    PaiementTransport addPaiement(PaiementTransport paiementTransport);
    PaiementTransport updatePaiement(PaiementTransport paiementTransport);
    void deletePaiement(Long id);
    PaiementTransport getPaiementById(Long id);
    List<PaiementTransport> getAllPaiements();
    List<PaiementTransport> getPaiementsByStatut(PaiementStatut statut);
    PaiementTransport completePaiement(Long id);
    PaiementTransport refundPaiement(Long id);

    BigDecimal getPlateformeSolde();
}
