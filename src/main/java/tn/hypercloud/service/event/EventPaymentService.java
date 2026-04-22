package tn.hypercloud.service.event;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.hypercloud.dto.event.EventPaymentRequest;
import tn.hypercloud.dto.event.EventPaymentResponse;
import tn.hypercloud.entity.event.EventPayment;
import tn.hypercloud.entity.event.EventReservation;
import tn.hypercloud.repository.event.EventPaymentRepository;
import tn.hypercloud.repository.event.EventActivityRepository;
import tn.hypercloud.repository.event.EventReservationRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import tn.hypercloud.entity.user.User;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j  // ← Ajoute @Slf4j ici
public class EventPaymentService {

    private final EventPaymentRepository repository;
    private final EventReservationRepository reservationRepository;
    private final EventActivityRepository eventRepository;
    private final EventEmailService emailService;
        private final EventTicketService ticketService;

    @Value("${app.payment.mock:false}")
    private boolean mockPayment;
    @Transactional(readOnly = true)
    public List<EventPaymentResponse> getAll() {
        return repository.findAll()
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public EventPaymentResponse getById(Integer id) {
        return toResponse(repository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("Payment not found : " + id)));
    }

    @Transactional(readOnly = true)
    public EventPaymentResponse getByReservation(
            Integer reservationId) {
        return toResponse(repository
                .findByReservationId(reservationId)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Payment not found for reservation : "
                                        + reservationId)));
    }

    public EventPaymentResponse create(
            EventPaymentRequest request) {

        EventReservation reservation = reservationRepository
                .findById(request.getReservationId())
                .orElseThrow(() ->
                        new RuntimeException(
                                "Reservation not found : "
                                        + request.getReservationId()));

        if (repository.existsByReservationId(
                request.getReservationId())) {
            throw new RuntimeException(
                    "Reservation already paid");
        }

        if (request.getAmount().compareTo(
                reservation.getTotalPrice()) != 0) {
            throw new RuntimeException(
                    "Amount does not match reservation price");
        }

        EventPayment payment = EventPayment.builder()
                .amount(request.getAmount())
                .paymentMethod(request.getPaymentMethod())
                .paymentStatus(EventPayment.PaymentStatus.PENDING)
                .transactionId(request.getTransactionId())
                .currency(request.getCurrency())
                .reservation(reservation)
                .build();

        return toResponse(repository.save(payment));
    }

    public EventPaymentResponse success(Integer id) {
        EventPayment payment = repository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("Payment not found : " + id));

        payment.setPaymentStatus(
                EventPayment.PaymentStatus.SUCCESS);
        payment.setPaymentDate(LocalDateTime.now());

        EventReservation reservation = payment.getReservation();
        reservation.setStatus(
                EventReservation.ReservationStatus.CONFIRMED);
        reservationRepository.save(reservation);
        ticketService.ensureTicketsGenerated(reservation);

        // 🔧 Envoyer email après paiement réussi
        try {
            sendConfirmationEmail(reservation);
        } catch (Exception e) {
            log.warn("Email send failed for reservation {}: {}",
                    reservation.getId(), e.getMessage());
        }

        return toResponse(repository.save(payment));
    }
    @Transactional
    public EventPaymentResponse mockPaymentSuccess(Integer reservationId) {
        if (!mockPayment) {
            throw new RuntimeException("Mock payment not enabled");
        }

        log.info("⚡ MOCK MODE: Paiement simulé pour réservation {}", reservationId);

        EventReservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() ->
                        new RuntimeException("Reservation not found : " + reservationId));

        EventPayment payment = repository.findByReservationId(reservationId)
                .orElseGet(() -> EventPayment.builder()
                        .reservation(reservation)
                        .amount(reservation.getTotalPrice())
                        .paymentMethod("MOCK")
                        .currency("TND")
                        .build());

        payment.setPaymentStatus(EventPayment.PaymentStatus.SUCCESS);
        payment.setPaymentDate(LocalDateTime.now());

        reservation.setStatus(EventReservation.ReservationStatus.CONFIRMED);
        reservationRepository.save(reservation);
        ticketService.ensureTicketsGenerated(reservation);

        try {
            sendConfirmationEmail(reservation);
        } catch (Exception e) {
            log.warn("Email send failed for mock payment {}: {}",
                    reservationId, e.getMessage());
        }

        return toResponse(repository.save(payment));
    }

    private void sendConfirmationEmail(EventReservation reservation) {
        User user = reservation.getUser();
        if (user == null || user.getEmail() == null) return;

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        String eventDate = reservation.getEvent().getStartDate().format(formatter);

        emailService.sendReservationConfirmation(
                user.getEmail(),
                user.getFullName(),
                reservation.getEvent().getTitle(),
                eventDate,
                reservation.getEvent().getAddress() + ", " +
                        reservation.getEvent().getCity(),
                reservation.getNumberOfTickets(),
                reservation.getTotalPrice(),
                reservation.getId()
        );
    }

    public EventPaymentResponse failed(Integer id) {
        EventPayment payment = repository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("Payment not found : " + id));
        payment.setPaymentStatus(
                EventPayment.PaymentStatus.FAILED);
        return toResponse(repository.save(payment));
    }

    public EventPaymentResponse refund(Integer id) {
        EventPayment payment = repository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("Payment not found : " + id));

        if (payment.getPaymentStatus() !=
                EventPayment.PaymentStatus.SUCCESS) {
            throw new RuntimeException(
                    "Cannot refund a payment that is not SUCCESS");
        }

        payment.setPaymentStatus(
                EventPayment.PaymentStatus.REFUNDED);

        EventReservation reservation = payment.getReservation();
        reservation.setStatus(
                EventReservation.ReservationStatus.CANCELLED);
        ticketService.cancelTicketsForReservation(reservation.getId());
        reservation.getEvent().setAvailableSeats(
                reservation.getEvent().getAvailableSeats() +
                        reservation.getNumberOfTickets());

        reservationRepository.save(reservation);
        eventRepository.save(reservation.getEvent());
        return toResponse(repository.save(payment));
    }

    public void delete(Integer id) {
        repository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("Payment not found : " + id));
        repository.deleteById(id);
    }

    private EventPaymentResponse toResponse(EventPayment p) {
        return EventPaymentResponse.builder()
                .id(p.getId())
                .amount(p.getAmount())
                .paymentMethod(p.getPaymentMethod())
                .paymentStatus(p.getPaymentStatus() != null ?
                        p.getPaymentStatus().name() : null)
                .transactionId(p.getTransactionId())
                .paymentDate(p.getPaymentDate())
                .currency(p.getCurrency())
                .createdAt(p.getCreatedAt())
                .reservationId(p.getReservation() != null ?
                        p.getReservation().getId() : null)
                .eventTitle(p.getReservation() != null &&
                        p.getReservation().getEvent() != null ?
                        p.getReservation().getEvent().getTitle() : null)
                .userName(p.getReservation() != null &&
                        p.getReservation().getUser() != null ?
                        p.getReservation().getUser().getFullName() : null)
                .build();
    }
}