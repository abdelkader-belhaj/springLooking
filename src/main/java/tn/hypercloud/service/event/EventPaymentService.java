package tn.hypercloud.service.event;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.hypercloud.dto.event.EventPaymentRequest;
import tn.hypercloud.dto.event.EventPaymentResponse;
import tn.hypercloud.dto.event.StripeCheckoutSessionRequest;
import tn.hypercloud.dto.event.StripeCheckoutSessionResponse;
import tn.hypercloud.dto.event.StripeConfirmRequest;
import tn.hypercloud.entity.event.EventPayment;
import tn.hypercloud.entity.event.EventReservation;
import tn.hypercloud.repository.event.EventPaymentRepository;
import tn.hypercloud.repository.event.EventActivityRepository;
import tn.hypercloud.repository.event.EventReservationRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import tn.hypercloud.entity.event.EventActivity;
import java.time.LocalDateTime;
import java.math.RoundingMode;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import tn.hypercloud.entity.user.User;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j  // ← Ajoute @Slf4j ici
public class EventPaymentService {

        private static final String STRIPE_CHECKOUT_API = "https://api.stripe.com/v1/checkout/sessions";
        private static final String STRIPE_SESSION_API = "https://api.stripe.com/v1/checkout/sessions/";

    private final EventPaymentRepository repository;
    private final EventReservationRepository reservationRepository;
    private final EventActivityRepository eventRepository;
    private final EventEmailService emailService;
        private final EventTicketService ticketService;

        private final RestTemplate restTemplate = new RestTemplate();
        private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${app.payment.mock:false}")
    private boolean mockPayment;

        @Value("${app.payment.stripe.secret-key:}")
        private String stripeSecretKey;

        @Value("${app.payment.stripe.exchange-rate-tnd-to-eur:0.30}")
        private BigDecimal tndToEurRate;

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

        public StripeCheckoutSessionResponse createStripeCheckoutSession(
                        StripeCheckoutSessionRequest request,
                        String email) {

                EventReservation reservation = reservationRepository
                                .findById(request.getReservationId())
                                .orElseThrow(() -> new RuntimeException("Reservation not found : " + request.getReservationId()));

                User caller = reservation.getUser();
                if (caller == null || !Objects.equals(caller.getEmail(), email)) {
                        throw new RuntimeException("Not authorized to pay for this reservation");
                }

                BigDecimal payableAmount = resolveStripeAmount(reservation, request.getPromoCode());
                String checkoutCurrency = resolveCheckoutCurrency();
                BigDecimal checkoutAmount = convertAmountForStripe(payableAmount, checkoutCurrency);
                EventPayment payment = repository.findByReservationId(reservation.getId())
                                .orElseGet(() -> EventPayment.builder()
                                                .reservation(reservation)
                                                .paymentStatus(EventPayment.PaymentStatus.PENDING)
                                                .build());

                if (payment.getPaymentStatus() == EventPayment.PaymentStatus.SUCCESS) {
                        throw new RuntimeException("Reservation already paid");
                }

                StripeSessionPayload session = createStripeSession(reservation, checkoutAmount, checkoutCurrency);

                payment.setAmount(checkoutAmount);
                payment.setPaymentMethod("STRIPE");
                payment.setPaymentStatus(EventPayment.PaymentStatus.PENDING);
                payment.setTransactionId(session.sessionId());
                payment.setCurrency(checkoutCurrency);
                repository.save(payment);

                return StripeCheckoutSessionResponse.builder()
                                .sessionId(session.sessionId())
                                .checkoutUrl(session.checkoutUrl())
                                .reservationId(reservation.getId())
                                .paymentId(payment.getId())
                                .build();
        }

        public EventPaymentResponse confirmStripeCheckoutSession(
                        StripeConfirmRequest request,
                        String email) {

                EventReservation reservation = reservationRepository
                                .findById(request.getReservationId())
                                .orElseThrow(() -> new RuntimeException("Reservation not found : " + request.getReservationId()));

                User caller = reservation.getUser();
                if (caller == null || !Objects.equals(caller.getEmail(), email)) {
                        throw new RuntimeException("Not authorized to confirm this payment");
                }

                EventPayment payment = repository.findByReservationId(reservation.getId())
                                .orElseThrow(() -> new RuntimeException("Payment not found for reservation : " + reservation.getId()));

                if (request.getSessionId() == null || request.getSessionId().isBlank()) {
                        throw new RuntimeException("Stripe sessionId is required");
                }

                if (payment.getTransactionId() == null || !payment.getTransactionId().equals(request.getSessionId().trim())) {
                        throw new RuntimeException("Stripe session mismatch");
                }

                StripeSessionStatus session = fetchStripeSession(request.getSessionId().trim());
                if (!"paid".equalsIgnoreCase(session.paymentStatus())) {
                        payment.setPaymentStatus(EventPayment.PaymentStatus.FAILED);
                        repository.save(payment);
                        throw new RuntimeException("Stripe payment is not confirmed yet");
                }

                return success(payment.getId());
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

        private StripeSessionPayload createStripeSession(EventReservation reservation, BigDecimal payableAmount, String checkoutCurrency) {
                if (stripeSecretKey == null || stripeSecretKey.isBlank()) {
                        throw new RuntimeException("Stripe is not configured");
                }

                String successUrl = "http://localhost:4200"
                                + "/mes-reservations-event?reservationId=" + reservation.getId()
                                + "&stripeSessionId={CHECKOUT_SESSION_ID}";
                String cancelUrl = "http://localhost:4200"
                                + "/mes-reservations-event?reservationId=" + reservation.getId();

                HttpHeaders headers = new HttpHeaders();
                headers.setBasicAuth(stripeSecretKey, "");
                headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

                MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
                body.add("mode", "payment");
                body.add("success_url", successUrl);
                body.add("cancel_url", cancelUrl);
                body.add("client_reference_id", String.valueOf(reservation.getId()));
                body.add("payment_method_types[]", "card");
                body.add("metadata[reservationId]", String.valueOf(reservation.getId()));
                body.add("metadata[eventId]", reservation.getEvent() != null && reservation.getEvent().getId() != null
                                ? String.valueOf(reservation.getEvent().getId()) : "");
                body.add("line_items[0][quantity]", "1");
                body.add("line_items[0][price_data][currency]", checkoutCurrency);
                body.add("line_items[0][price_data][unit_amount]", payableAmount.multiply(BigDecimal.valueOf(100)).setScale(0, RoundingMode.HALF_UP).toPlainString());
                body.add("line_items[0][price_data][product_data][name]", reservation.getEvent() != null ? reservation.getEvent().getTitle() : "Event ticket");
                body.add("line_items[0][price_data][product_data][description]", buildStripeDescription(reservation));

                ResponseEntity<String> response = restTemplate.postForEntity(
                                STRIPE_CHECKOUT_API,
                                new HttpEntity<>(body, headers),
                                String.class);

                if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                        throw new RuntimeException("Stripe checkout session creation failed");
                }

                try {
                        JsonNode root = objectMapper.readTree(response.getBody());
                        String sessionId = root.path("id").asText("");
                        String checkoutUrl = root.path("url").asText("");
                        if (sessionId.isBlank() || checkoutUrl.isBlank()) {
                                throw new RuntimeException("Stripe response missing url");
                        }
                        return new StripeSessionPayload(sessionId, checkoutUrl);
                } catch (Exception ex) {
                        throw new RuntimeException("Stripe session parsing failed: " + ex.getMessage(), ex);
                }
        }

        private StripeSessionStatus fetchStripeSession(String sessionId) {
                if (stripeSecretKey == null || stripeSecretKey.isBlank()) {
                        throw new RuntimeException("Stripe is not configured");
                }

                HttpHeaders headers = new HttpHeaders();
                headers.setBasicAuth(stripeSecretKey, "");
                ResponseEntity<String> response = restTemplate.exchange(
                                STRIPE_SESSION_API + sessionId,
                                org.springframework.http.HttpMethod.GET,
                                new HttpEntity<>(headers),
                                String.class);

                try {
                        JsonNode root = objectMapper.readTree(response.getBody() == null ? "{}" : response.getBody());
                        String paymentStatus = root.path("payment_status").asText("");
                        return new StripeSessionStatus(paymentStatus);
                } catch (Exception ex) {
                        throw new RuntimeException("Stripe session verification failed: " + ex.getMessage(), ex);
                }
        }

        private BigDecimal resolveStripeAmount(EventReservation reservation, String promoCode) {
                BigDecimal baseAmount = reservation.getTotalPrice();
                if (promoCode == null || promoCode.isBlank()) {
                        return baseAmount;
                }

                EventActivity event = reservation.getEvent();
                if (event == null) {
                        return baseAmount;
                }

                String expected = event.getPromoCode() == null ? "" : event.getPromoCode().trim().toLowerCase();
                String provided = promoCode.trim().toLowerCase();
                if (expected.isBlank() || !expected.equals(provided)) {
                        throw new RuntimeException("Code promo invalide ou expiré.");
                }

                LocalDateTime now = LocalDateTime.now();
                if (event.getPromoStartDate() != null && now.isBefore(event.getPromoStartDate())) {
                        throw new RuntimeException("Code promo pas encore valable.");
                }

                if (event.getPromoEndDate() != null && now.isAfter(event.getPromoEndDate())) {
                        throw new RuntimeException("Code promo expiré.");
                }

                Integer percent = event.getPromoPercent();
                if (percent == null || percent <= 0) {
                        throw new RuntimeException("Code promo invalide ou expiré.");
                }

                BigDecimal discount = baseAmount.multiply(BigDecimal.valueOf(percent)).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
                return baseAmount.subtract(discount).setScale(2, RoundingMode.HALF_UP);
        }

        private BigDecimal convertAmountForStripe(BigDecimal tndAmount, String checkoutCurrency) {
                if (tndAmount == null) {
                        return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
                }

                if ("eur".equalsIgnoreCase(checkoutCurrency)) {
                        BigDecimal rate = tndToEurRate == null || tndToEurRate.compareTo(BigDecimal.ZERO) <= 0
                                        ? new BigDecimal("0.30")
                                        : tndToEurRate;
                        return tndAmount.multiply(rate).setScale(2, RoundingMode.HALF_UP);
                }

                return tndAmount.setScale(2, RoundingMode.HALF_UP);
        }

        private String resolveCheckoutCurrency() {
                return "eur";
        }

        private String buildStripeDescription(EventReservation reservation) {
                String title = reservation.getEvent() != null ? reservation.getEvent().getTitle() : "Event";
                int tickets = reservation.getNumberOfTickets();
                String client = reservation.getUser() != null ? reservation.getUser().getFullName() : "Client";
                return title + " - " + client + " - " + tickets + " billet(s)";
        }

        private String normalizeCurrency(String value) {
                if (value == null || value.isBlank()) {
                        return "eur";
                }
                return value.trim().toLowerCase();
        }

        private record StripeSessionPayload(String sessionId, String checkoutUrl) {}
        private record StripeSessionStatus(String paymentStatus) {}
}