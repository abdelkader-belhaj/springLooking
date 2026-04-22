package tn.hypercloud.service.event;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.hypercloud.dto.event.EventReservationRequest;
import tn.hypercloud.dto.event.EventReservationResponse;
import tn.hypercloud.dto.event.EventVisionAnalyzeRequest;
import tn.hypercloud.dto.event.EventVisionAnalyzeResponse;
import tn.hypercloud.entity.event.EventActivity;
import tn.hypercloud.entity.event.EventPayment;
import tn.hypercloud.entity.event.EventReservation;
import tn.hypercloud.entity.user.Role;
import tn.hypercloud.entity.user.User;
import tn.hypercloud.exception.GlobalExceptionHandler.ApiException;
import tn.hypercloud.repository.event.EventActivityRepository;
import tn.hypercloud.repository.event.EventPaymentRepository;
import tn.hypercloud.repository.event.EventReservationRepository;
import tn.hypercloud.repository.user.UserRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EventReservationService {

    private final EventReservationRepository repository;
    private final EventActivityRepository eventRepository;
    private final UserRepository userRepository;
    private final EventPaymentRepository paymentRepository;
    private final EventVisionAiService visionAiService;
    private final EventTicketService ticketService;

    public List<EventReservationResponse> getAll() {
        return repository.findAll()
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * CLIENT : uniquement sa réservation.
     * ORGANISATEUR : réservation liée à un de ses événements.
     * ADMIN : toute réservation.
     */
    @Transactional(readOnly = true)
    public EventReservationResponse getById(Integer id, String email) {
        EventReservation reservation = repository.findById(id)
                .orElseThrow(() ->
                        new ApiException(HttpStatus.NOT_FOUND,
                                "Reservation not found : " + id));

        User caller = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new ApiException(HttpStatus.NOT_FOUND, "User not found"));

        if (caller.getRole() == Role.ADMIN) {
            return toResponse(reservation);
        }
        if (caller.getRole() == Role.ORGANISATEUR) {
            if (!reservation.getEvent().getOrganizer().getId().equals(caller.getId())) {
                throw new ApiException(HttpStatus.FORBIDDEN,
                        "Not authorized to view this reservation");
            }
            return toResponse(reservation);
        }
        if (caller.getRole() == Role.CLIENT_TOURISTE) {
            if (!reservation.getUser().getId().equals(caller.getId())) {
                throw new ApiException(HttpStatus.FORBIDDEN,
                        "Not authorized to view this reservation");
            }
            return toResponse(reservation);
        }

        throw new ApiException(HttpStatus.FORBIDDEN,
                "Not authorized to view this reservation");
    }

    public List<EventReservationResponse> getMesReservations(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new ApiException(HttpStatus.NOT_FOUND, "User not found"));

        return repository.findByUserId(user.getId())
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * ORGANISATEUR : uniquement les réservations de ses propres événements.
     * ADMIN : toutes les réservations de l'événement.
     */
    @Transactional(readOnly = true)
    public List<EventReservationResponse> getByEvent(Integer eventId, String email) {
        EventActivity event = eventRepository.findById(eventId)
                .orElseThrow(() ->
                        new ApiException(HttpStatus.NOT_FOUND,
                                "Event not found : " + eventId));

        User caller = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new ApiException(HttpStatus.NOT_FOUND, "User not found"));

        if (caller.getRole() == Role.ADMIN) {
            return listByEventId(eventId);
        }
        if (caller.getRole() == Role.ORGANISATEUR) {
            if (!event.getOrganizer().getId().equals(caller.getId())) {
                throw new ApiException(HttpStatus.FORBIDDEN,
                        "Not authorized to view reservations for this event");
            }
            return listByEventId(eventId);
        }

        throw new ApiException(HttpStatus.FORBIDDEN,
                "Not authorized to view reservations for this event");
    }

    private List<EventReservationResponse> listByEventId(Integer eventId) {
        return repository.findByEventId(eventId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public EventReservationResponse create(EventReservationRequest request, String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new ApiException(HttpStatus.NOT_FOUND, "User not found"));

        EventActivity event = eventRepository.findById(request.getEventId())
                .orElseThrow(() ->
                        new ApiException(HttpStatus.NOT_FOUND,
                                "Event not found : " + request.getEventId()));

        if (event.getStatus() != EventActivity.EventStatus.PUBLISHED) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Event is not published");
        }

        if (event.getAvailableSeats() < request.getNumberOfTickets()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Not enough seats available");
        }

        if (repository.existsByEventIdAndUserId(request.getEventId(), user.getId())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Already reserved this event");
        }

        BigDecimal totalPrice = event.getPrice()
                .multiply(BigDecimal.valueOf(request.getNumberOfTickets()));

        EventReservation reservation = EventReservation.builder()
                .numberOfTickets(request.getNumberOfTickets())
                .totalPrice(totalPrice)
                .status(EventReservation.ReservationStatus.PENDING)
                .event(event)
                .user(user)
                .build();

        event.setAvailableSeats(event.getAvailableSeats() - request.getNumberOfTickets());
        eventRepository.save(event);

        EventReservation savedReservation = repository.save(reservation);
        return toResponse(savedReservation);
    }

    @Transactional
    public EventReservationResponse cancel(Integer id, String email) {
        EventReservation reservation = repository.findById(id)
                .orElseThrow(() ->
                        new ApiException(HttpStatus.NOT_FOUND,
                                "Reservation not found : " + id));

        if (!reservation.getUser().getEmail().equals(email)) {
            throw new ApiException(HttpStatus.FORBIDDEN,
                    "Not authorized to cancel this reservation");
        }

        if (reservation.getStatus() == EventReservation.ReservationStatus.CANCELLED) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "Reservation is already cancelled");
        }

        EventActivity event = reservation.getEvent();
        event.setAvailableSeats(event.getAvailableSeats() + reservation.getNumberOfTickets());
        eventRepository.save(event);

        reservation.setStatus(EventReservation.ReservationStatus.CANCELLED);
        ticketService.cancelTicketsForReservation(reservation.getId());
        return toResponse(repository.save(reservation));
    }

    public void delete(Integer id) {
        repository.findById(id)
                .orElseThrow(() ->
                        new ApiException(HttpStatus.NOT_FOUND,
                                "Reservation not found : " + id));
        repository.deleteById(id);
    }

    private EventReservationResponse toResponse(EventReservation r) {
        return EventReservationResponse.builder()
                .id(r.getId())
                .reservationDate(r.getReservationDate())
                .numberOfTickets(r.getNumberOfTickets())
                .totalPrice(r.getTotalPrice())
                .status(r.getStatus() != null ? r.getStatus().name() : null)
                .eventId(r.getEvent() != null ? r.getEvent().getId() : null)
                .eventTitle(r.getEvent() != null ? r.getEvent().getTitle() : null)
                .eventPrice(r.getEvent() != null ? r.getEvent().getPrice() : null)
                .userId(r.getUser() != null ? r.getUser().getId() : null)
                .userName(r.getUser() != null ? r.getUser().getFullName() : null)
                .qrUsed(r.isQrUsed())
                .qrUsedAt(r.getQrUsedAt())
                .qrUsedBy(r.getQrUsedBy())
                .build();
    }

    @Transactional
    public Map<String, Object> scanQr(Integer id, String scannerEmail) {
        EventReservation r = repository.findByIdForUpdate(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Réservation introuvable"));

        syncReservationStatusFromPayment(r);

        if (r.getStatus() != EventReservation.ReservationStatus.CONFIRMED) {
            return Map.of("valid", false, "message", "Réservation non confirmée ❌");
        }

        List<tn.hypercloud.dto.event.EventTicketResponse> tickets =
                ticketService.getTicketsByReservation(id, scannerEmail);
        long usedCount = tickets.stream().filter(t -> t.isUsed()).count();
        if (usedCount >= tickets.size() && !tickets.isEmpty()) {
            return Map.of("valid", false, "message", "Tous les tickets de cette réservation sont déjà utilisés ❌");
        }

        String usedBy = (scannerEmail == null || scannerEmail.isBlank()) ? "unknown" : scannerEmail.trim();

        if (tickets.isEmpty()) {
            r.setQrUsed(true);
            r.setQrUsedAt(LocalDateTime.now());
            r.setQrUsedBy(usedBy);
            repository.save(r);
            return Map.of("valid", true, "message", "Billet valide ✅");
        }

        tn.hypercloud.dto.event.EventTicketResponse nextTicket = tickets.stream()
                .filter(t -> !t.isUsed())
                .findFirst()
                .orElse(null);

        if (nextTicket == null || nextTicket.getTicketCode() == null) {
            return Map.of("valid", false, "message", "Aucun ticket disponible à valider ❌");
        }

        Map<String, Object> scanResult = ticketService.scanTicketByCode(nextTicket.getTicketCode(), scannerEmail);
        ticketService.syncReservationQrState(id);

        return scanResult;
    }

    @Transactional
    public EventVisionAnalyzeResponse analyzeTicketWithAi(EventVisionAnalyzeRequest request, String scannerEmail) {
    EventReservation reservation = repository.findByIdForUpdate(request.getReservationId())
        .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Réservation introuvable"));

    syncReservationStatusFromPayment(reservation);

    User scanner = userRepository.findByEmail(scannerEmail)
        .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Scanner introuvable"));

    if (scanner.getRole() == Role.ORGANISATEUR
        && !reservation.getEvent().getOrganizer().getId().equals(scanner.getId())) {
        throw new ApiException(HttpStatus.FORBIDDEN,
            "Non autorisé à valider cette réservation");
    }

    if (reservation.isQrUsed()) {
        return EventVisionAnalyzeResponse.builder()
            .valid(false)
            .message("Billet déjà utilisé ❌")
            .reservationId(reservation.getId())
            .alreadyUsed(true)
            .build();
    }

    if (reservation.getStatus() != EventReservation.ReservationStatus.CONFIRMED) {
        return EventVisionAnalyzeResponse.builder()
            .valid(false)
            .message("Réservation non confirmée ❌")
            .reservationId(reservation.getId())
            .alreadyUsed(false)
            .build();
    }

    EventVisionAiService.VisionAnalysisResult aiResult =
        visionAiService.analyzeTicketImage(request.getImageBase64(), reservation);

    Integer detectedId = aiResult.getReservationIdDetected();
    boolean idMismatch = detectedId != null && !reservation.getId().equals(detectedId);

    if (!aiResult.isValid() || idMismatch) {
        String mismatchMessage = idMismatch
            ? "Le ticket analysé ne correspond pas à cette réservation."
            : aiResult.getMessage();

        return EventVisionAnalyzeResponse.builder()
            .valid(false)
            .message(mismatchMessage)
            .extractedData(aiResult.getExtractedData())
            .reservationId(reservation.getId())
            .alreadyUsed(false)
            .build();
    }

    String usedBy = (scannerEmail == null || scannerEmail.isBlank()) ? "unknown" : scannerEmail.trim();
    reservation.setQrUsed(true);
    reservation.setQrUsedAt(LocalDateTime.now());
    reservation.setQrUsedBy(usedBy);
    repository.save(reservation);
    ticketService.syncReservationQrState(reservation.getId());

    return EventVisionAnalyzeResponse.builder()
        .valid(true)
        .message("Billet validé avec succès ✅")
        .extractedData(aiResult.getExtractedData())
        .reservationId(reservation.getId())
        .alreadyUsed(false)
        .build();
    }

    private void syncReservationStatusFromPayment(EventReservation reservation) {
        if (reservation == null || reservation.getId() == null) {
            return;
        }
        if (reservation.getStatus() == EventReservation.ReservationStatus.CONFIRMED
                || reservation.getStatus() == EventReservation.ReservationStatus.CANCELLED) {
            return;
        }

        paymentRepository.findByReservationId(reservation.getId())
                .filter(payment -> payment.getPaymentStatus() == EventPayment.PaymentStatus.SUCCESS)
                .ifPresent(payment -> {
                    reservation.setStatus(EventReservation.ReservationStatus.CONFIRMED);
                    repository.save(reservation);
                    ticketService.ensureTicketsGenerated(reservation);
                });
    }
}