package tn.hypercloud.controller.transport;

import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.hypercloud.entity.transport.PaiementTransport;
import tn.hypercloud.service.transport.*;

import java.math.BigDecimal;
import java.util.List;
@RestController
@RequestMapping("/hypercloud/paiements")
@AllArgsConstructor
public class PaiementController {

    private final IPaiementService paiementService;

    @PostMapping
    public PaiementTransport addPaiement(@RequestBody PaiementTransport paiementTransport) {
        return paiementService.addPaiement(paiementTransport);
    }

    @PutMapping("/{id}")
    public PaiementTransport updatePaiement(@PathVariable Long id, @RequestBody PaiementTransport paiementTransport) {
        paiementTransport.setIdPaiement(id);
        return paiementService.updatePaiement(paiementTransport);
    }

    @DeleteMapping("/{id}")
    public void deletePaiement(@PathVariable Long id) {
        paiementService.deletePaiement(id);
    }

    @GetMapping("/{id}")
    public PaiementTransport getPaiementById(@PathVariable Long id) {
        return paiementService.getPaiementById(id);
    }

    @GetMapping
    public List<PaiementTransport> getAllPaiements() {
        return paiementService.getAllPaiements();
    }

    @PutMapping("/{id}/completer")
    public PaiementTransport completePaiement(@PathVariable Long id) {
        return paiementService.completePaiement(id);
    }

    @PutMapping("/{id}/rembourser")
    public PaiementTransport refundPaiement(@PathVariable Long id) {
        return paiementService.refundPaiement(id);
    }
    @GetMapping("/plateforme/solde")
    public ResponseEntity<BigDecimal> getPlateformeSolde() {
        BigDecimal solde = paiementService.getPlateformeSolde();
        return ResponseEntity.ok(solde);
    }
}