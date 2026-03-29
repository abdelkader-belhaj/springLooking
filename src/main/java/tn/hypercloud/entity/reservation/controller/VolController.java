package tn.hypercloud.entity.reservation.controller;



import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import tn.hypercloud.entity.reservation.dto.VolRequest;
import tn.hypercloud.entity.reservation.dto.VolResponse;
import tn.hypercloud.entity.reservation.service.VolService;

import java.time.LocalDate;
import java.util.List;
@CrossOrigin(origins = "http://localhost:4200")
@RestController
@RequestMapping("/api/vols")
@RequiredArgsConstructor



public class VolController {

    private final VolService volService;

    // =============================================
    //  SOCIETE : CRUD complet sur les vols
    // =============================================

    @PostMapping
    @PreAuthorize("hasRole('SOCIETE')")
    public ResponseEntity<VolResponse> create(
            @AuthenticationPrincipal UserDetails user, // ← ajouter
            @RequestBody VolRequest req) {
        return ResponseEntity.ok(volService.create(user.getUsername(), req)); // ← passer l'email
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('SOCIETE')")
    public ResponseEntity<VolResponse> update(@PathVariable Integer id,
                                              @RequestBody VolRequest req) {
        return ResponseEntity.ok(volService.update(id, req));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('SOCIETE')")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        volService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('SOCIETE','CLIENT_TOURISTE')")
    public ResponseEntity<VolResponse> getById(@PathVariable Integer id) {
        return ResponseEntity.ok(volService.getById(id));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('SOCIETE','CLIENT_TOURISTE')")
    public ResponseEntity<List<VolResponse>> getAll() {
        return ResponseEntity.ok(volService.getAll());
    }

    @GetMapping("/mes-vols")
    @PreAuthorize("hasRole('SOCIETE')")
    public ResponseEntity<List<VolResponse>> getMesVols(
            @AuthenticationPrincipal UserDetails user) {
        Long userId = /* récupérer userId */ null;
        return ResponseEntity.ok(volService.getByUser(userId));
    }

    // =============================================
    //  CLIENT : Recherche de vols disponibles
    // =============================================

    @GetMapping("/search")
    @PreAuthorize("hasRole('CLIENT_TOURISTE')")
    public ResponseEntity<List<VolResponse>> search(
            @RequestParam String depart,
            @RequestParam String arrivee,
            @RequestParam String date) {
        return ResponseEntity.ok(volService.search(depart, arrivee, LocalDate.parse(date)));
    }
}