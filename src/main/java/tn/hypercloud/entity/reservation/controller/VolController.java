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
import tn.hypercloud.entity.user.User;
import tn.hypercloud.repository.user.UserRepository;

import java.time.LocalDate;
import java.util.List;

@CrossOrigin(origins = "http://localhost:4200")
@RestController
@RequestMapping("/api/vols")
@RequiredArgsConstructor
public class VolController {

    private final VolService volService;
    private final UserRepository userRepository; // ✅ ajouté

    // ==============================
    // SOCIETE : CRUD
    // ==============================

    @PostMapping
    @PreAuthorize("hasRole('SOCIETE')")
    public ResponseEntity<VolResponse> create(
            @AuthenticationPrincipal UserDetails user,
            @RequestBody VolRequest req) {

        return ResponseEntity.ok(volService.create(user.getUsername(), req));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('SOCIETE')")
    public ResponseEntity<VolResponse> update(
            @PathVariable Integer id,
            @AuthenticationPrincipal UserDetails user,
            @RequestBody VolRequest req) {

        return ResponseEntity.ok(volService.update(id, user.getUsername(), req));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('SOCIETE')")
    public ResponseEntity<Void> delete(
            @PathVariable Integer id,
            @AuthenticationPrincipal UserDetails user) {

        volService.delete(id, user.getUsername());
        return ResponseEntity.noContent().build();
    }

    // ==============================
    // TOUS (CLIENT + SOCIETE)
    // ==============================

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

    // ==============================
    // SOCIETE : ses vols seulement
    // ==============================

    @GetMapping("/mes-vols")
    @PreAuthorize("hasRole('SOCIETE')")
    public ResponseEntity<List<VolResponse>> getMesVols(
            @AuthenticationPrincipal UserDetails user) {

        User u = userRepository.findByEmail(user.getUsername())
                .orElseThrow(() -> new RuntimeException("Utilisateur introuvable"));

        return ResponseEntity.ok(volService.getByUser(u.getId()));
    }

    // ==============================
    // CLIENT : recherche
    // ==============================

    @GetMapping("/search")
    @PreAuthorize("hasRole('CLIENT_TOURISTE')")
    public ResponseEntity<List<VolResponse>> search(
            @RequestParam String depart,
            @RequestParam String arrivee,
            @RequestParam String date) {

        return ResponseEntity.ok(
                volService.search(depart, arrivee, LocalDate.parse(date))
        );
    }
}