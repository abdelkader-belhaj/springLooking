package tn.hypercloud.controller.accommodation;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import tn.hypercloud.payload.request.ReservationRequest;
import tn.hypercloud.payload.response.ReservationResponse;
import tn.hypercloud.service.accommodation.ReservationLogementService;

import java.util.List;

@RestController
@RequestMapping("/api/reservations-logement")
@RequiredArgsConstructor
public class ReservationLogementController {

    private final ReservationLogementService service;

    // CREATE RESERVATION
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','CLIENT_TOURISTE')")
    public ResponseEntity<ReservationResponse> create(
            @RequestBody ReservationRequest req) {

        return ResponseEntity.ok(service.create(req));
    }

    // GET ALL RESERVATIONS
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','HEBERGEUR','CLIENT_TOURISTE')")
    public ResponseEntity<List<ReservationResponse>> getAll() {

        return ResponseEntity.ok(service.getAll());
    }

    // GET RESERVATION BY ID
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','HEBERGEUR','CLIENT_TOURISTE')")
    public ResponseEntity<ReservationResponse> getById(
            @PathVariable Integer id) {

        return ResponseEntity.ok(service.getById(id));
    }

    // MODIFIER RESERVATION
    @PutMapping("/{id}/modifier")
    @PreAuthorize("hasRole('CLIENT_TOURISTE')")
    public ResponseEntity<ReservationResponse> modifier(
            @PathVariable Integer id,
            @RequestBody ReservationRequest req) {

        return ResponseEntity.ok(service.modifier(id, req));
    }

    // ANNULER RESERVATION
    @PatchMapping("/{id}/annuler")
    @PreAuthorize("hasAnyRole('ADMIN','HEBERGEUR','CLIENT_TOURISTE')")
    public ResponseEntity<ReservationResponse> annuler(

            @PathVariable Integer id) {

        return ResponseEntity.ok(service.annuler(id));
    }

    // DELETE RESERVATION
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(
            @PathVariable Integer id) {

        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    // ARCHIVE RESERVATION
    @PatchMapping("/{id}/archiver")
    @PreAuthorize("hasAnyRole('ADMIN','HEBERGEUR')")
    public ResponseEntity<ReservationResponse> archive(
            @PathVariable Integer id) {

        return ResponseEntity.ok(service.archive(id));
    }

    // UNARCHIVE RESERVATION
    @PatchMapping("/{id}/desarchiver")
    @PreAuthorize("hasAnyRole('ADMIN','HEBERGEUR')")
    public ResponseEntity<ReservationResponse> unarchive(
            @PathVariable Integer id) {

        return ResponseEntity.ok(service.unarchive(id));
    }
}