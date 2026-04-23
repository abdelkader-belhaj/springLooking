package tn.hypercloud.entity.reservation.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import tn.hypercloud.entity.reservation.dto.ReservationResponse;
import tn.hypercloud.entity.reservation.service.AnnulationVolService;

import java.util.List;

@CrossOrigin(origins = "http://localhost:4200")
@RestController
@RequestMapping("/api/annulations")
@RequiredArgsConstructor
public class AnnulationVolController {

    private final AnnulationVolService annulationService;

    // ============================================================
    //  CLIENT : DEMANDER ANNULATION + REMBOURSEMENT
    //  POST /api/annulations/{id}
    //  - Vérifie que la réservation est active et payée
    //  - Vérifie la règle des 48h avant départ
    //  - Restitue les places
    //  - Marque paiement → rembourse
    //  - Marque réservation → annulee
    // ============================================================
    @PostMapping("/{id}")
    @PreAuthorize("hasRole('CLIENT_TOURISTE')")
    public ResponseEntity<ReservationResponse> demanderAnnulation(
            @AuthenticationPrincipal UserDetails user,
            @PathVariable Integer id) {
        return ResponseEntity.ok(
                annulationService.demanderAnnulation(user.getUsername(), id)
        );
    }

    // ============================================================
    //  SOCIÉTÉ : VOIR TOUTES LES ANNULATIONS
    //  GET /api/annulations
    // ============================================================
    @GetMapping
    @PreAuthorize("hasRole('SOCIETE')")
    public ResponseEntity<List<ReservationResponse>> toutesLesAnnulations() {
        return ResponseEntity.ok(annulationService.toutesLesAnnulations());
    }

    // ============================================================
    //  SOCIÉTÉ : CONFIRMER REMBOURSEMENT MANUELLEMENT
    //  PUT /api/annulations/{id}/confirmer-remboursement
    //  Utile avant intégration Flouci ou pour forcer le statut
    // ============================================================
    @PutMapping("/{id}/confirmer-remboursement")
    @PreAuthorize("hasRole('SOCIETE')")
    public ResponseEntity<ReservationResponse> confirmerRemboursement(
            @PathVariable Integer id) {
        return ResponseEntity.ok(
                annulationService.confirmerRemboursement(id)
        );
    }
}