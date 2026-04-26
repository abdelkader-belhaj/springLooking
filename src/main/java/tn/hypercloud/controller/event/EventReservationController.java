package tn.hypercloud.controller.event;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import tn.hypercloud.dto.event.EventReservationRequest;
import tn.hypercloud.dto.event.EventReservationResponse;
import tn.hypercloud.dto.event.EventVisionAnalyzeRequest;
import tn.hypercloud.dto.event.EventVisionAnalyzeResponse;
import tn.hypercloud.service.event.EventEmailService;
import tn.hypercloud.service.event.EventReservationService;

import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;

@RestController
@RequestMapping("/api/reservations/events")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:4200")
@Validated
public class EventReservationController {

    private final EventReservationService service;
    private final EventEmailService emailService;
    @Value("${app.frontend.base-url:http://localhost:4200}")
    private String frontendBaseUrl;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<EventReservationResponse>> getAll() {
        return ResponseEntity.ok(service.getAll());
    }

    @GetMapping("/mes-reservations-event")
    @PreAuthorize("hasRole('CLIENT_TOURISTE')")
    public ResponseEntity<List<EventReservationResponse>> getMesReservationsEvent(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(
                service.getMesReservationsEvent(
                        userDetails.getUsername()));
    }

    @GetMapping("/event/{eventId}")
    @PreAuthorize("hasAnyRole('ORGANISATEUR','ADMIN')")
    public ResponseEntity<List<EventReservationResponse>> getByEvent(
            @PathVariable Integer eventId,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(
                service.getByEvent(eventId, userDetails.getUsername()));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('CLIENT_TOURISTE','ORGANISATEUR','ADMIN')")
    public ResponseEntity<EventReservationResponse> getById(
            @PathVariable Integer id,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(
                service.getById(id, userDetails.getUsername()));
    }

    @PostMapping
    @PreAuthorize("hasRole('CLIENT_TOURISTE')")
    public ResponseEntity<EventReservationResponse> create(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody EventReservationRequest request) {
        return ResponseEntity.ok(
                service.create(request, userDetails.getUsername()));
    }

    @PutMapping("/{id}/cancel")
    @PreAuthorize("hasRole('CLIENT_TOURISTE')")
    public ResponseEntity<EventReservationResponse> cancel(
            @PathVariable Integer id,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(
                service.cancel(id, userDetails.getUsername()));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(
            @PathVariable Integer id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/scan-qr")
    @PreAuthorize("hasAnyRole('ADMIN','ORGANISATEUR')")
    public ResponseEntity<Map<String,Object>> scanQr(
            @PathVariable Integer id,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(service.scanQr(id, userDetails.getUsername()));
    }

    @PostMapping("/analyze-ticket")
    @PreAuthorize("hasAnyRole('ADMIN','ORGANISATEUR')")
    public ResponseEntity<EventVisionAnalyzeResponse> analyzeTicketWithAi(
            @Valid @RequestBody EventVisionAnalyzeRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(service.analyzeTicketWithAi(request, userDetails.getUsername()));
    }
}
