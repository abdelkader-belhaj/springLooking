package tn.hypercloud.controller.event;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import tn.hypercloud.dto.event.EventPaymentRequest;
import tn.hypercloud.dto.event.EventPaymentResponse;
import tn.hypercloud.dto.event.StripeCheckoutSessionRequest;
import tn.hypercloud.dto.event.StripeCheckoutSessionResponse;
import tn.hypercloud.dto.event.StripeConfirmRequest;
import tn.hypercloud.service.event.EventPaymentService;
import java.util.List;

@RestController
@RequestMapping("/api/payments/events")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:4200")
public class EventPaymentController {

    private final EventPaymentService service;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<EventPaymentResponse>> getAll() {
        return ResponseEntity.ok(service.getAll());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('CLIENT_TOURISTE','ORGANISATEUR','ADMIN')")
    public ResponseEntity<EventPaymentResponse> getById(
            @PathVariable Integer id) {
        return ResponseEntity.ok(service.getById(id));
    }

    @GetMapping("/reservation/{reservationId}")
    @PreAuthorize("hasAnyRole('CLIENT_TOURISTE','ORGANISATEUR','ADMIN')")
    public ResponseEntity<EventPaymentResponse> getByReservation(
            @PathVariable Integer reservationId) {
        return ResponseEntity.ok(
                service.getByReservation(reservationId));
    }

    @PostMapping
    @PreAuthorize("hasRole('CLIENT_TOURISTE')")
    public ResponseEntity<EventPaymentResponse> create(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody EventPaymentRequest request) {
        return ResponseEntity.ok(service.create(request));
    }

    @PostMapping("/stripe/session")
    @PreAuthorize("hasRole('CLIENT_TOURISTE')")
    public ResponseEntity<StripeCheckoutSessionResponse> createStripeSession(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody StripeCheckoutSessionRequest request) {
        return ResponseEntity.ok(service.createStripeCheckoutSession(request, userDetails.getUsername()));
    }

    @PostMapping("/stripe/confirm")
    @PreAuthorize("hasRole('CLIENT_TOURISTE')")
    public ResponseEntity<EventPaymentResponse> confirmStripeSession(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody StripeConfirmRequest request) {
        return ResponseEntity.ok(service.confirmStripeCheckoutSession(request, userDetails.getUsername()));
    }

    @PutMapping("/{id}/success")
    @PreAuthorize("hasAnyRole('CLIENT_TOURISTE','ADMIN')")
    public ResponseEntity<EventPaymentResponse> success(
            @PathVariable Integer id) {
        return ResponseEntity.ok(service.success(id));
    }

    @PutMapping("/{id}/failed")
    @PreAuthorize("hasAnyRole('CLIENT_TOURISTE','ADMIN')")
    public ResponseEntity<EventPaymentResponse> failed(
            @PathVariable Integer id) {
        return ResponseEntity.ok(service.failed(id));
    }

    @PutMapping("/{id}/refund")
    @PreAuthorize("hasAnyRole('ORGANISATEUR','ADMIN')")
    public ResponseEntity<EventPaymentResponse> refund(
            @PathVariable Integer id) {
        return ResponseEntity.ok(service.refund(id));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(
            @PathVariable Integer id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
