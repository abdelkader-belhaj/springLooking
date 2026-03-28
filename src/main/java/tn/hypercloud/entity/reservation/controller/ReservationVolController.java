package tn.hypercloud.entity.reservation.controller;



import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import tn.hypercloud.entity.reservation.dto.PaiementRequest;
import tn.hypercloud.entity.reservation.dto.ReservationRequest;
import tn.hypercloud.entity.reservation.dto.ReservationResponse;
import tn.hypercloud.entity.reservation.service.ReservationVolService;

import java.util.List;
@CrossOrigin(origins = "http://localhost:4200")
@RestController
@RequestMapping("/api/reservations")
@RequiredArgsConstructor




public class ReservationVolController {

    private final ReservationVolService reservationService;

    // =============================================
    //  CLIENT_TOURISTE : CRUD sur ses réservations
    // =============================================

    /** Créer une réservation aller simple ou aller-retour */
    @PostMapping
    @PreAuthorize("hasRole('CLIENT_TOURISTE')")
    public ResponseEntity<ReservationResponse> creer(
            @AuthenticationPrincipal UserDetails user,
            @RequestBody ReservationRequest req) {
        return ResponseEntity.ok(reservationService.creer(user.getUsername(), req));
    }

    /** Voir mes réservations */
    @GetMapping("/mes-reservations")
    @PreAuthorize("hasRole('CLIENT_TOURISTE')")
    public ResponseEntity<List<ReservationResponse>> mesReservations(
            @AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(reservationService.mesReservations(user.getUsername()));
    }

    /** Annuler une réservation */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('CLIENT_TOURISTE')")
    public ResponseEntity<Void> annuler(
            @AuthenticationPrincipal UserDetails user,
            @PathVariable Integer id) {
        reservationService.annuler(user.getUsername(), id);
        return ResponseEntity.noContent().build();
    }

    /** Payer une réservation (statique - à remplacer par Flouci/Stripe) */
    @PostMapping("/payer")
    @PreAuthorize("hasRole('CLIENT_TOURISTE')")
    public ResponseEntity<ReservationResponse> payer(
            @AuthenticationPrincipal UserDetails user,
            @RequestBody PaiementRequest req) {
        return ResponseEntity.ok(reservationService.payer(user.getUsername(), req));
    }
}