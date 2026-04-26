package tn.hypercloud.entity.reservation.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import tn.hypercloud.entity.reservation.dto.ReservationRequest;
import tn.hypercloud.entity.reservation.dto.ReservationResponse;
import tn.hypercloud.entity.reservation.dto.StripePaymentRequest; // ← MODIFIÉ
import tn.hypercloud.entity.reservation.service.ReservationVolService;

import java.util.List;

@CrossOrigin(origins = "http://localhost:4200")
@RestController
@RequestMapping("/api/reservations")
@RequiredArgsConstructor
public class ReservationVolController {

    private final ReservationVolService reservationService;

    // ============================================================
    //  CLIENT : CRÉER UNE RÉSERVATION
    //  POST /api/reservations
    // ============================================================
    @PostMapping
    @PreAuthorize("hasRole('CLIENT_TOURISTE')")
    public ResponseEntity<ReservationResponse> creer(
            @AuthenticationPrincipal UserDetails user,
            @RequestBody ReservationRequest req) {
        return ResponseEntity.ok(reservationService.creer(user.getUsername(), req));
    }

    // ============================================================
    //  CLIENT : MES RÉSERVATIONS
    //  GET /api/reservations/mes-reservations
    // ============================================================
    @GetMapping("/mes-reservations")
    @PreAuthorize("hasRole('CLIENT_TOURISTE')")
    public ResponseEntity<List<ReservationResponse>> mesReservations(
            @AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(
                reservationService.mesReservations(user.getUsername()));
    }

    // ============================================================
    //  CLIENT : ANNULER UNE RÉSERVATION (suppression simple)
    //  DELETE /api/reservations/{id}
    // ============================================================
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('CLIENT_TOURISTE')")
    public ResponseEntity<Void> annuler(
            @AuthenticationPrincipal UserDetails user,
            @PathVariable Integer id) {
        reservationService.annuler(user.getUsername(), id);
        return ResponseEntity.noContent().build();
    }

    // ============================================================
    //  CLIENT : PAYER AVEC STRIPE
    //  POST /api/reservations/payer
    //  Body: { "reservationId": 1, "methode": "carte", "paymentMethodId": "pm_xxx" }
    // ============================================================
    @PostMapping("/payer")
    @PreAuthorize("hasRole('CLIENT_TOURISTE')")
    public ResponseEntity<ReservationResponse> payer(
            @AuthenticationPrincipal UserDetails user,
            @RequestBody StripePaymentRequest req) { // ← MODIFIÉ
        return ResponseEntity.ok(reservationService.payer(user.getUsername(), req));
    }

    // ============================================================
    //  SOCIÉTÉ : TOUTES LES RÉSERVATIONS
    //  GET /api/reservations/toutes
    // ============================================================
    @GetMapping("/toutes")
    @PreAuthorize("hasRole('SOCIETE')")
    public ResponseEntity<List<ReservationResponse>> toutesLesReservations() {
        return ResponseEntity.ok(reservationService.toutesLesReservations());
    }

    // ============================================================
    //  SOCIÉTÉ : MODIFIER STATUT
    //  PUT /api/reservations/{id}/statut?statut=paye
    // ============================================================
    @PutMapping("/{id}/statut")
    @PreAuthorize("hasRole('SOCIETE')")
    public ResponseEntity<ReservationResponse> modifierStatut(
            @PathVariable Integer id,
            @RequestParam String statut) {
        return ResponseEntity.ok(reservationService.modifierStatut(id, statut));
    }

    // ============================================================
    //  SOCIÉTÉ : SUPPRIMER UNE RÉSERVATION
    //  DELETE /api/reservations/admin/{id}
    // ============================================================
    @DeleteMapping("/admin/{id}")
    @PreAuthorize("hasRole('SOCIETE')")
    public ResponseEntity<ReservationResponse> supprimerReservation(@PathVariable Integer id) {
        return ResponseEntity.ok(reservationService.supprimerReservation(id));
    }
}