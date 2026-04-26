package tn.hypercloud.service.event;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.hypercloud.dto.event.EventTicketResponse;
import tn.hypercloud.entity.event.EventReservation;
import tn.hypercloud.entity.event.EventTicket;
import tn.hypercloud.entity.user.Role;
import tn.hypercloud.entity.user.User;
import tn.hypercloud.exception.GlobalExceptionHandler.ApiException;
import tn.hypercloud.repository.event.EventReservationRepository;
import tn.hypercloud.repository.event.EventTicketRepository;
import tn.hypercloud.repository.user.UserRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EventTicketService {

    private final EventTicketRepository ticketRepository;
    private final EventReservationRepository reservationRepository;
    private final UserRepository userRepository;
    private final EventScanAiNarrationService scanAiNarrationService;

    @Transactional
    public void ensureTicketsGenerated(EventReservation reservation) {
        if (reservation == null || reservation.getId() == null) {
            return;
        }
        if (reservation.getStatus() != EventReservation.ReservationStatus.CONFIRMED) {
            return;
        }
        if (reservation.getNumberOfTickets() <= 0) {
            return;
        }
        if (ticketRepository.existsByReservationId(reservation.getId())) {
            return;
        }

        for (int i = 1; i <= reservation.getNumberOfTickets(); i++) {
            EventTicket ticket = EventTicket.builder()
                    .ticketCode(buildTicketCode(reservation.getId(), i))
                    .ticketNumber(i)
                    .status(EventTicket.TicketStatus.AVAILABLE)
                    .used(false)
                    .reservation(reservation)
                    .build();
            ticketRepository.save(ticket);
        }
    }

    @Transactional
    public void cancelTicketsForReservation(Integer reservationId) {
        if (reservationId == null) {
            return;
        }

        List<EventTicket> tickets = ticketRepository.findByReservationIdOrderByTicketNumberAsc(reservationId);
        for (EventTicket ticket : tickets) {
            if (ticket.getStatus() != EventTicket.TicketStatus.CANCELLED) {
                ticket.setStatus(EventTicket.TicketStatus.CANCELLED);
                ticketRepository.save(ticket);
            }
        }
    }

    @Transactional
    public List<EventTicketResponse> getTicketsByReservation(Integer reservationId, String email) {
        EventReservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Reservation not found : " + reservationId));

        User caller = userRepository.findByEmail(email)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found"));

        boolean isAdmin = caller.getRole() == Role.ADMIN;
        boolean isOrganizerOwner = caller.getRole() == Role.ORGANISATEUR
                && reservation.getEvent().getOrganizer().getId().equals(caller.getId());
        boolean isClientOwner = caller.getRole() == Role.CLIENT_TOURISTE
                && reservation.getUser().getId().equals(caller.getId());

        if (!isAdmin && !isOrganizerOwner && !isClientOwner) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Not authorized to view tickets");
        }

        // Self-heal legacy reservations: if payment is confirmed but tickets are missing,
        // generate them before returning data to the client UI.
        ensureTicketsGenerated(reservation);

        return ticketRepository.findByReservationIdOrderByTicketNumberAsc(reservationId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public Map<String, Object> scanTicketByCode(String ticketCode, String scannerEmail) {
        String normalizedCode = ticketCode == null ? "" : ticketCode.trim();
        if (normalizedCode.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "ticketCode is required");
        }

        EventTicket ticket = ticketRepository.findByTicketCodeForUpdate(normalizedCode)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Ticket not found"));

        EventReservation reservation = ticket.getReservation();

        User scanner = userRepository.findByEmail(scannerEmail)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Scanner user not found"));

        boolean isAdmin = scanner.getRole() == Role.ADMIN;
        boolean isOrganizerOwner = scanner.getRole() == Role.ORGANISATEUR
                && reservation.getEvent().getOrganizer().getId().equals(scanner.getId());

        if (!isAdmin && !isOrganizerOwner) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Not authorized to scan this ticket");
        }

        if (reservation.getStatus() != EventReservation.ReservationStatus.CONFIRMED) {
            return Map.of("valid", false, "message", "Reservation non confirmee");
        }

        if (ticket.getStatus() == EventTicket.TicketStatus.CANCELLED) {
            return Map.of("valid", false, "message", "Ticket annule");
        }

        if (ticket.isUsed()) {
            String smartMessage = scanAiNarrationService.buildScanMessage(
                    new EventScanAiNarrationService.ScanNarrationContext(
                            false,
                            true,
                            reservation.getUser() != null ? reservation.getUser().getFullName() : "Client",
                            reservation.getEvent() != null ? reservation.getEvent().getTitle() : "Événement",
                            ticket.getTicketCode(),
                            ticket.getUsedBy(),
                            LocalDateTime.now(),
                            ticket.getUsedAt()
                    )
            );
            return Map.of("valid", false, "message", smartMessage);
        }

        LocalDateTime now = LocalDateTime.now();
        ticket.setUsed(true);
        ticket.setUsedAt(now);
        ticket.setUsedBy(scannerEmail);
        ticket.setStatus(EventTicket.TicketStatus.USED);
        ticketRepository.save(ticket);

        syncReservationQrState(reservation.getId());

        String smartMessage = scanAiNarrationService.buildScanMessage(
                new EventScanAiNarrationService.ScanNarrationContext(
                        true,
                        false,
                        reservation.getUser() != null ? reservation.getUser().getFullName() : "Client",
                        reservation.getEvent() != null ? reservation.getEvent().getTitle() : "Événement",
                        ticket.getTicketCode(),
                        scannerEmail,
                        now,
                        now
                )
        );

        return Map.of(
                "valid", true,
                "message", smartMessage,
                "ticketCode", ticket.getTicketCode(),
                "ticketNumber", ticket.getTicketNumber(),
                "reservationId", reservation.getId()
        );
    }

    @Transactional
    public void syncReservationQrState(Integer reservationId) {
        EventReservation reservation = reservationRepository.findByIdForUpdate(reservationId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Reservation not found : " + reservationId));

        long total = ticketRepository.countByReservationId(reservationId);
        long used = ticketRepository.countByReservationIdAndUsedTrue(reservationId);

        if (total > 0 && used >= total) {
            reservation.setQrUsed(true);
            if (reservation.getQrUsedAt() == null) {
                reservation.setQrUsedAt(LocalDateTime.now());
            }
            if (reservation.getQrUsedBy() == null || reservation.getQrUsedBy().isBlank()) {
                reservation.setQrUsedBy("multiple-scans");
            }
        } else {
            reservation.setQrUsed(false);
            reservation.setQrUsedAt(null);
            reservation.setQrUsedBy(null);
        }
        reservationRepository.save(reservation);
    }

    private String buildTicketCode(Integer reservationId, int ticketNumber) {
        String random = UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
        return "EVT-" + reservationId + "-" + ticketNumber + "-" + random;
    }

    private EventTicketResponse toResponse(EventTicket ticket) {
        EventReservation reservation = ticket.getReservation();
        return EventTicketResponse.builder()
                .id(ticket.getId())
                .ticketCode(ticket.getTicketCode())
                .ticketNumber(ticket.getTicketNumber())
                .status(ticket.getStatus() != null ? ticket.getStatus().name() : null)
                .used(ticket.isUsed())
                .usedAt(ticket.getUsedAt())
                .usedBy(ticket.getUsedBy())
                .reservationId(reservation != null ? reservation.getId() : null)
                .eventId(reservation != null && reservation.getEvent() != null ? reservation.getEvent().getId() : null)
                .eventTitle(reservation != null && reservation.getEvent() != null ? reservation.getEvent().getTitle() : null)
                .ownerUserId(reservation != null && reservation.getUser() != null ? reservation.getUser().getId() : null)
                .ownerName(reservation != null && reservation.getUser() != null ? reservation.getUser().getFullName() : null)
                .build();
    }
}