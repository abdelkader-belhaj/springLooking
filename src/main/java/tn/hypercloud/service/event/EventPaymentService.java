package tn.hypercloud.service.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.hypercloud.dto.event.EventPaymentRequest;
import tn.hypercloud.dto.event.EventPaymentResponse;
import tn.hypercloud.entity.event.EventActivity;
import tn.hypercloud.entity.event.EventPayment;
import tn.hypercloud.entity.event.EventReservation;
import tn.hypercloud.entity.user.User;
import tn.hypercloud.repository.event.EventActivityRepository;
import tn.hypercloud.repository.event.EventPaymentRepository;
import tn.hypercloud.repository.event.EventReservationRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
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
    public EventPaymentResponse getByReservation(Integer reservationId) {
        return toResponse(repository
                .findByReservationId(reservationId)
                .orElseThrow(() ->
                        new RuntimeException("Payment not found for reservation : " + reservationId)));
    }

    public EventPaymentResponse create(EventPaymentRequest request) {
        EventReservation reservation = reservationRepository
                .findById(request.getReservationId())
                .orElseThrow(() ->
                        new RuntimeException("Reservation not found : " + request.getReservationId()));

        EventPayment payment = repository.findByReservationId(request.getReservationId()).orElse(null);
        if (payment != null && payment.getPaymentStatus() == EventPayment.PaymentStatus.SUCCESS) {
            throw new RuntimeException("Reservation already paid");
        }

        BigDecimal expectedAmount = resolvePayableAmount(reservation, request.getPromoCode());
        BigDecimal requestedAmount = normalizeAmount(request.getAmount(), expectedAmount);
        if (requestedAmount.compareTo(expectedAmount) != 0) {
            throw new RuntimeException("Amount does not match expected reservation price");
        }

        if (payment == null) {
            payment = EventPayment.builder()
                    .reservation(reservation)
                    .build();
        }

        payment.setAmount(expectedAmount);
        payment.setPaymentMethod(normalizePaymentMethod(request.getPaymentMethod()));
        payment.setPaymentStatus(EventPayment.PaymentStatus.PENDING);
        payment.setTransactionId(normalizeTransactionId(request.getTransactionId(), reservation.getId()));
        payment.setCurrency(normalizeCurrency(request.getCurrency()));

        return toResponse(repository.save(payment));
    }

    public EventPaymentResponse success(Integer id) {
        log.info("★ EventPaymentService.success() called with payment ID: {}", id);
        EventPayment payment = repository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("Payment not found : " + id));

        payment.setPaymentStatus(EventPayment.PaymentStatus.SUCCESS);
        payment.setPaymentDate(LocalDateTime.now());

        EventReservation reservation = payment.getReservation();
        log.info("★ Setting reservation {} to CONFIRMED status", reservation.getId());
        reservation.setStatus(EventReservation.ReservationStatus.CONFIRMED);
        reservationRepository.save(reservation);
        ticketService.ensureTicketsGenerated(reservation);

        try {
            log.info("★ Calling sendConfirmationEmail for reservation {}", reservation.getId());
            sendConfirmationEmail(reservation);
            log.info("★ Email sent successfully for reservation {}", reservation.getId());
        } catch (Exception e) {
            log.error("★ Email send FAILED for reservation {}", reservation.getId(), e);
        }

        return toResponse(repository.save(payment));
    }

    @Transactional
    public EventPaymentResponse mockPaymentSuccess(Integer reservationId) {
        if (!mockPayment) {
            throw new RuntimeException("Mock payment not enabled");
        }

        log.info("MOCK MODE: simulated payment for reservation {}", reservationId);

        EventReservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() ->
                        new RuntimeException("Reservation not found : " + reservationId));

        EventPayment payment = repository.findByReservationId(reservationId)
                .orElseGet(() -> EventPayment.builder()
                        .reservation(reservation)
                        .amount(reservation.getTotalPrice())
                        .paymentMethod("EVENT_STATIC")
                        .currency("tnd")
                        .build());

        payment.setPaymentStatus(EventPayment.PaymentStatus.SUCCESS);
        payment.setPaymentDate(LocalDateTime.now());

        reservation.setStatus(EventReservation.ReservationStatus.CONFIRMED);
        reservationRepository.save(reservation);
        ticketService.ensureTicketsGenerated(reservation);

        try {
            sendConfirmationEmail(reservation);
        } catch (Exception e) {
            log.warn("Email send failed for mock payment {}",
                    reservationId, e);
        }

        return toResponse(repository.save(payment));
    }

    private void sendConfirmationEmail(EventReservation reservation) {
        log.info("★ sendConfirmationEmail() START for reservation {}", reservation.getId());
        User user = reservation.getUser();
        if (user == null || user.getEmail() == null || user.getEmail().isBlank()) {
            log.warn("★ Skip confirmation email for reservation {} because user/email is missing (user={}, email={})", 
                    reservation.getId(), user, user != null ? user.getEmail() : "N/A");
            return;
        }

        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
            String eventDate = reservation.getEvent().getStartDate().format(formatter);

            log.info("★ About to send email to {} with subject: Confirmation de réservation pour {}", 
                    user.getEmail(), reservation.getEvent().getTitle());
            
            emailService.sendReservationConfirmation(
                    user.getEmail(),
                    user.getFullName(),
                    reservation.getEvent().getTitle(),
                    eventDate,
                    reservation.getEvent().getAddress() + ", " + reservation.getEvent().getCity(),
                    reservation.getNumberOfTickets(),
                    reservation.getTotalPrice(),
                    reservation.getId()
            );
            
            log.info("★ sendConfirmationEmail() SUCCESS for reservation {}", reservation.getId());
        } catch (Exception e) {
            log.error("★ sendConfirmationEmail() EXCEPTION for reservation {}", reservation.getId(), e);
            throw new RuntimeException("Failed to send confirmation email", e);
        }
    }

    public EventPaymentResponse failed(Integer id) {
        EventPayment payment = repository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("Payment not found : " + id));
        payment.setPaymentStatus(EventPayment.PaymentStatus.FAILED);
        return toResponse(repository.save(payment));
    }

    public EventPaymentResponse refund(Integer id) {
        EventPayment payment = repository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("Payment not found : " + id));

        if (payment.getPaymentStatus() != EventPayment.PaymentStatus.SUCCESS) {
            throw new RuntimeException("Cannot refund a payment that is not SUCCESS");
        }

        payment.setPaymentStatus(EventPayment.PaymentStatus.REFUNDED);

        EventReservation reservation = payment.getReservation();
        reservation.setStatus(EventReservation.ReservationStatus.CANCELLED);
        ticketService.cancelTicketsForReservation(reservation.getId());
        reservation.getEvent().setAvailableSeats(
                reservation.getEvent().getAvailableSeats() + reservation.getNumberOfTickets());

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
                .paymentStatus(p.getPaymentStatus() != null ? p.getPaymentStatus().name() : null)
                .transactionId(p.getTransactionId())
                .paymentDate(p.getPaymentDate())
                .currency(p.getCurrency())
                .createdAt(p.getCreatedAt())
                .reservationId(p.getReservation() != null ? p.getReservation().getId() : null)
                .eventTitle(p.getReservation() != null && p.getReservation().getEvent() != null
                        ? p.getReservation().getEvent().getTitle() : null)
                .userName(p.getReservation() != null && p.getReservation().getUser() != null
                        ? p.getReservation().getUser().getFullName() : null)
                .build();
    }

    private BigDecimal resolvePayableAmount(EventReservation reservation, String promoCode) {
        BigDecimal baseAmount = reservation.getTotalPrice() == null
                ? BigDecimal.ZERO
                : reservation.getTotalPrice();

        if (promoCode == null || promoCode.isBlank()) {
            return baseAmount.setScale(2, RoundingMode.HALF_UP);
        }

        EventActivity event = reservation.getEvent();
        if (event == null) {
            return baseAmount.setScale(2, RoundingMode.HALF_UP);
        }

        String expected = event.getPromoCode() == null ? "" : event.getPromoCode().trim().toLowerCase();
        String provided = promoCode.trim().toLowerCase();
        if (expected.isBlank() || !expected.equals(provided)) {
            throw new RuntimeException("Code promo invalide ou expire.");
        }

        LocalDateTime now = LocalDateTime.now();
        if (event.getPromoStartDate() != null && now.isBefore(event.getPromoStartDate())) {
            throw new RuntimeException("Code promo pas encore valable.");
        }

        if (event.getPromoEndDate() != null && now.isAfter(event.getPromoEndDate())) {
            throw new RuntimeException("Code promo expire.");
        }

        Integer percent = event.getPromoPercent();
        if (percent == null || percent <= 0) {
            throw new RuntimeException("Code promo invalide ou expire.");
        }

        BigDecimal discount = baseAmount.multiply(BigDecimal.valueOf(percent))
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        return baseAmount.subtract(discount).setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal normalizeAmount(BigDecimal requested, BigDecimal fallback) {
        BigDecimal source = requested == null ? fallback : requested;
        return source.setScale(2, RoundingMode.HALF_UP);
    }

    private String normalizeCurrency(String value) {
        if (value == null || value.isBlank()) {
            return "tnd";
        }
        return value.trim().toLowerCase();
    }

    private String normalizePaymentMethod(String value) {
        if (value == null || value.isBlank()) {
            return "EVENT_STATIC";
        }
        return value.trim().toUpperCase();
    }

    private String normalizeTransactionId(String tx, Integer reservationId) {
        if (tx != null && !tx.isBlank()) {
            return tx.trim();
        }
        return "EVT-STATIC-" + reservationId + "-" + System.currentTimeMillis();
    }
}
