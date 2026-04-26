package tn.hypercloud.controller.event;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import tn.hypercloud.dto.event.EventTicketResponse;
import tn.hypercloud.entity.event.EventReservation;
import tn.hypercloud.repository.event.EventReservationRepository;
import tn.hypercloud.service.event.EventTicketService;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/tickets/events")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:4200")
public class EventTicketController {

    private final EventTicketService ticketService;
    private final EventReservationRepository reservationRepository; // ← ajoute ça


    @GetMapping("/reservation/{reservationId}")
    @PreAuthorize("hasAnyRole('CLIENT_TOURISTE','ORGANISATEUR','ADMIN')")
    public ResponseEntity<List<EventTicketResponse>> getByReservation(
            @PathVariable Integer reservationId,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(ticketService.getTicketsByReservation(reservationId, userDetails.getUsername()));
    }

    @PostMapping("/scan/{ticketCode}")
    @PreAuthorize("hasAnyRole('ORGANISATEUR','ADMIN')")
    public ResponseEntity<Map<String, Object>> scanByCode(
            @PathVariable String ticketCode,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(ticketService.scanTicketByCode(ticketCode, userDetails.getUsername()));
    }
    @PostMapping("/admin/fix-missing-tickets")
    @PreAuthorize("hasAnyRole('ORGANISATEUR','ADMIN')")
    public ResponseEntity<String> fixMissingTickets() {
        List<EventReservation> confirmed = reservationRepository
                .findByStatus(EventReservation.ReservationStatus.CONFIRMED);

        int fixedCount = 0;
        for (EventReservation r : confirmed) {
            boolean missingBefore = r.getTickets() == null || r.getTickets().isEmpty();
            ticketService.ensureTicketsGenerated(r);
            if (missingBefore) {
                fixedCount++;
            }
        }

        return ResponseEntity.ok("Tickets verifies. Missing tickets generated for " + fixedCount + " confirmed reservations.");
    }
}
