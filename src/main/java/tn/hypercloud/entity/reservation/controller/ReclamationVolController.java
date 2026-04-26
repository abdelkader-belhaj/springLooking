package tn.hypercloud.entity.reservation.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpServletRequest;
import tn.hypercloud.entity.reservation.dto.*;
import tn.hypercloud.entity.reservation.service.ReclamationVolService;

import java.util.ArrayList;
import java.util.List;

@CrossOrigin(origins = "http://localhost:4200")
@RestController
@RequestMapping("/api/reclamations")
@RequiredArgsConstructor
public class ReclamationVolController {

    private final ReclamationVolService reclamationService;

    // ============================================================
    //  CLIENT : CRÉER UNE RÉCLAMATION
    //  POST /api/reclamations
    // ============================================================
    @PostMapping
    @PreAuthorize("hasRole('CLIENT_TOURISTE')")
    public ResponseEntity<ReclamationResponse> creer(
            @AuthenticationPrincipal UserDetails user,
            @Valid @RequestBody ReclamationCreateRequest req) {
        return ResponseEntity.ok(reclamationService.creer(user.getUsername(), req));
    }

    // ============================================================
    //  CLIENT : MES RÉCLAMATIONS
    //  GET /api/reclamations/mes
    // ============================================================
    @GetMapping("/mes")
    @PreAuthorize("hasRole('CLIENT_TOURISTE')")
    public ResponseEntity<List<ReclamationResponse>> mes(
            @AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(reclamationService.mesReclamations(user.getUsername()));
    }

    // ============================================================
    //  CLIENT : NOTIFICATIONS (NB RÉPONSES NON LUES)
    //  GET /api/reclamations/mes/unread-count
    // ============================================================
    @GetMapping("/mes/unread-count")
    @PreAuthorize("hasRole('CLIENT_TOURISTE')")
    public ResponseEntity<UnreadCountResponse> unreadCount(
            @AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(new UnreadCountResponse(
                reclamationService.unreadCount(user.getUsername())
        ));
    }

    // ============================================================
    //  CLIENT : MARQUER COMME LU
    //  PUT /api/reclamations/{id}/lu
    // ============================================================
    @PutMapping("/{id}/lu")
    @PreAuthorize("hasRole('CLIENT_TOURISTE')")
    public ResponseEntity<Void> marquerLu(
            @AuthenticationPrincipal UserDetails user,
            @PathVariable Integer id) {
        reclamationService.marquerLu(user.getUsername(), id);
        return ResponseEntity.noContent().build();
    }

    // ============================================================
    //  CLIENT : MODIFIER (uniquement si ouverte)
    //  PUT /api/reclamations/{id}
    // ============================================================
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('CLIENT_TOURISTE')")
    public ResponseEntity<ReclamationResponse> modifier(
            @AuthenticationPrincipal UserDetails user,
            @PathVariable Integer id,
            @Valid @RequestBody ReclamationUpdateRequest req) {
        return ResponseEntity.ok(reclamationService.modifier(user.getUsername(), id, req));
    }

    // ============================================================
    //  SOCIÉTÉ : LISTE DES RÉCLAMATIONS
    //  GET /api/reclamations
    // ============================================================
    @GetMapping
    @PreAuthorize("hasRole('SOCIETE')")
    public ResponseEntity<List<ReclamationResponse>> toutes(
            @AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(reclamationService.toutesPourSociete(user.getUsername()));
    }

    // ============================================================
    //  SOCIÉTÉ : RÉPONDRE
    //  PUT /api/reclamations/{id}/reponse
    // ============================================================
    @PutMapping("/{id}/reponse")
    @PreAuthorize("hasRole('SOCIETE')")
    public ResponseEntity<ReclamationResponse> repondre(
            @AuthenticationPrincipal UserDetails user,
            @PathVariable Integer id,
            @Valid @RequestBody ReclamationReplyRequest req) {
        return ResponseEntity.ok(reclamationService.repondre(user.getUsername(), id, req));
    }
}

