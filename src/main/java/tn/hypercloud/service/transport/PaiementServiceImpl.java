package tn.hypercloud.service.transport;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import tn.hypercloud.entity.transport.Paiement;
import tn.hypercloud.entity.transport.enums.PaiementStatut;
import tn.hypercloud.repository.transport.PaiementRepository;

import java.util.List;
@Service
@AllArgsConstructor
public class PaiementServiceImpl implements IPaiementService {
    private final PaiementRepository paiementRepository;

    @Override
    public Paiement addPaiement(Paiement paiement) {
        return paiementRepository.save(paiement);
    }

    @Override
    public Paiement updatePaiement(Paiement paiement) {
        return paiementRepository.save(paiement);
    }

    @Override
    public void deletePaiement(Long id) {
        paiementRepository.deleteById(id);
    }

    @Override
    public Paiement getPaiementById(Long id) {
        return paiementRepository.findById(id).orElse(null);
    }

    @Override
    public List<Paiement> getAllPaiements() {
        return paiementRepository.findAll();
    }

    @Override
    public List<Paiement> getPaiementsByStatut(PaiementStatut statut) {
        return paiementRepository.findByStatut(statut);
    }

    @Override
    public Paiement completePaiement(Long id) {
        Paiement paiement = getPaiementById(id);
        if (paiement == null) return null;
        paiement.setStatut(PaiementStatut.COMPLETED);
        return paiementRepository.save(paiement);
    }

    @Override
    public Paiement refundPaiement(Long id) {
        Paiement paiement = getPaiementById(id);
        if (paiement == null) return null;
        paiement.setStatut(PaiementStatut.REFUNDED);
        return paiementRepository.save(paiement);
    }
}