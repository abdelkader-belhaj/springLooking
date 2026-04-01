package tn.hypercloud.service.transport;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import tn.hypercloud.entity.transport.PaiementTransport;
import tn.hypercloud.entity.transport.enums.PaiementStatut;
import tn.hypercloud.repository.transport.PaiementRepository;

import java.math.BigDecimal;
import java.util.List;
@Service
@AllArgsConstructor
public class PaiementServiceImpl implements IPaiementService {
    private final PaiementRepository paiementRepository;

    @Override
    public PaiementTransport addPaiement(PaiementTransport paiementTransport) {
        return paiementRepository.save(paiementTransport);
    }

    @Override
    public PaiementTransport updatePaiement(PaiementTransport paiementTransport) {
        return paiementRepository.save(paiementTransport);
    }

    @Override
    public void deletePaiement(Long id) {
        paiementRepository.deleteById(id);
    }

    @Override
    public PaiementTransport getPaiementById(Long id) {
        return paiementRepository.findById(id).orElse(null);
    }

    @Override
    public List<PaiementTransport> getAllPaiements() {
        return paiementRepository.findAll();
    }

    @Override
    public List<PaiementTransport> getPaiementsByStatut(PaiementStatut statut) {
        return paiementRepository.findByStatut(statut);
    }

    @Override
    public PaiementTransport completePaiement(Long id) {
        PaiementTransport paiementTransport = getPaiementById(id);
        if (paiementTransport == null) return null;
        paiementTransport.setStatut(PaiementStatut.COMPLETED);
        return paiementRepository.save(paiementTransport);
    }

    @Override
    public PaiementTransport refundPaiement(Long id) {
        PaiementTransport paiementTransport = getPaiementById(id);
        if (paiementTransport == null) return null;
        paiementTransport.setStatut(PaiementStatut.REFUNDED);
        return paiementRepository.save(paiementTransport);
    }
    @Override
    public BigDecimal getPlateformeSolde() {
        return paiementRepository.sumMontantCommissionByStatut(PaiementStatut.COMPLETED);
    }
}