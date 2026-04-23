package tn.hypercloud.service.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.hypercloud.exception.GlobalExceptionHandler.ApiException;
import tn.hypercloud.dto.event.EventActivityRequest;
import tn.hypercloud.dto.event.EventActivityResponse;
import tn.hypercloud.dto.event.EventStatsResponse;
import tn.hypercloud.entity.event.EventActivity;
import tn.hypercloud.entity.event.EventCategory;
import tn.hypercloud.entity.event.EventPayment;
import tn.hypercloud.entity.event.EventReservation;
import tn.hypercloud.entity.user.User;
import tn.hypercloud.repository.event.EventActivityRepository;
import tn.hypercloud.repository.event.EventCategoryRepository;
import tn.hypercloud.repository.event.EventPaymentRepository;
import tn.hypercloud.repository.event.EventReservationRepository;
import tn.hypercloud.repository.user.UserRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class EventActivityService {

    private final EventActivityRepository repository;
    private final EventCategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final EventReservationRepository reservationRepository;
    private final EventPaymentRepository paymentRepository;
    private final EventEmailService emailService;
    private final SmsService smsService;
        private final EventTicketService ticketService;
        private final EventModerationAiService moderationAiService;


    // ============================================================
    // GET ALL
    // ============================================================
    public List<EventActivityResponse> getAll() {
        return repository.findAll()
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    // ============================================================
    // GET PUBLISHED — pour le catalogue Angular
    // ============================================================
    public List<EventActivityResponse> getPublished() {
        return repository
                .findByStatus(EventActivity.EventStatus.PUBLISHED)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Détail public : uniquement si statut {@link EventActivity.EventStatus#PUBLISHED}.
     */
    public EventActivityResponse getPublishedById(Integer id) {
        EventActivity event = repository.findById(id)
                .orElseThrow(() ->
                        new ApiException(HttpStatus.NOT_FOUND,
                                "Event not found : " + id));
        if (event.getStatus() != EventActivity.EventStatus.PUBLISHED) {
            throw new ApiException(HttpStatus.NOT_FOUND,
                    "Event not found : " + id);
        }
        return toResponse(event);
    }

    // ============================================================
    // GET BY ID
    // ============================================================
    public EventActivityResponse getById(Integer id) {
        return toResponse(repository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("Event not found : " + id)));
    }

    // ============================================================
    // GET BY TYPE
    // ============================================================
    public List<EventActivityResponse> getByType(String type) {
        return repository
                .findByType(EventActivity.EventType.valueOf(type))
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /** Catalogue public : même filtre que {@link #getPublished()}. */
    public List<EventActivityResponse> getPublishedByType(String type) {
        return repository
                .findByType(EventActivity.EventType.valueOf(type))
                .stream()
                .filter(e -> e.getStatus() == EventActivity.EventStatus.PUBLISHED)
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    // ============================================================
    // GET BY STATUS
    // ============================================================
    public List<EventActivityResponse> getByStatus(String status) {
        return repository
                .findByStatus(EventActivity.EventStatus.valueOf(status))
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    // ============================================================
    // GET BY CITY
    // ============================================================
    public List<EventActivityResponse> getByCity(String city) {
        return repository.findByCity(city)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public List<EventActivityResponse> getPublishedByCity(String city) {
        return repository.findByCity(city)
                .stream()
                .filter(e -> e.getStatus() == EventActivity.EventStatus.PUBLISHED)
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    // ============================================================
    // GET BY CATEGORY
    // ============================================================
    public List<EventActivityResponse> getByCategoryId(Integer categoryId) {
        return repository.findByCategoryId(categoryId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public List<EventActivityResponse> getPublishedByCategoryId(Integer categoryId) {
        return repository.findByCategoryId(categoryId)
                .stream()
                .filter(e -> e.getStatus() == EventActivity.EventStatus.PUBLISHED)
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    // ============================================================
    // GET MES EVENTS — pour l'organisateur connecté
    // ============================================================
    public List<EventActivityResponse> getMesEvents(String email) {
        User organizer = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new RuntimeException("User not found"));
        return repository.findByOrganizerId(organizer.getId())
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    // ============================================================
        // CREATE — ORGANISATEUR crée → modération auto (PUBLISHED / REJECTED)
    // ============================================================
    public EventActivityResponse create(
            EventActivityRequest request, String email) {

        // 1. Vérifier que l'organisateur existe
        User organizer = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new RuntimeException("User not found"));

        // 2. Vérifier que la catégorie existe
        EventCategory category = categoryRepository
                .findById(request.getCategoryId())
                .orElseThrow(() ->
                        new RuntimeException("Category not found"));

        ModerationResult moderation = evaluateModeration(request);

        // 3. Construire l'event avec statut auto
        EventActivity event = EventActivity.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .price(request.getPrice())
                .capacity(request.getCapacity())
                .availableSeats(request.getCapacity())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .city(request.getCity())
                .address(request.getAddress())
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .imageUrl(request.getImageUrl())
                .type(EventActivity.EventType
                        .valueOf(request.getType()))
                .status(moderation.approved()
                        ? EventActivity.EventStatus.PUBLISHED
                        : EventActivity.EventStatus.REJECTED)
                .moderatedAt(LocalDateTime.now())
                .moderatedByEmail("qwen3-auto")
                .moderationReason(moderation.reason())
                .promoType(resolvePromoType(request.getPromoType()))
                .promoPercent(normalizePromoPercent(request.getPromoPercent()))
                .promoCode(normalizePromoCode(request.getPromoCode()))
                .promoStartDate(request.getPromoStartDate())
                .promoEndDate(request.getPromoEndDate())
                .category(category)
                .organizer(organizer)
                .build();

        return toResponse(repository.save(event));
    }

    // ============================================================
    // UPDATE — ORGANISATEUR modifie (seulement si DRAFT)
    // ============================================================
    public EventActivityResponse update(
            Integer id,
            EventActivityRequest request,
            String email) {

        EventActivity event = repository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("Event not found : " + id));

        // Vérifier que c'est bien son event
        if (!event.getOrganizer().getEmail().equals(email)) {
            throw new RuntimeException(
                    "Not authorized to update this event");
        }

        // Seulement DRAFT ou REJECTED peuvent être modifiés puis re-modérés
        if (event.getStatus() != EventActivity.EventStatus.DRAFT
                && event.getStatus() != EventActivity.EventStatus.REJECTED) {
            throw new RuntimeException(
                    "Only DRAFT or REJECTED events can be updated");
        }

        ModerationResult moderation = evaluateModeration(request);

        event.setTitle(request.getTitle());
        event.setDescription(request.getDescription());
        event.setPrice(request.getPrice());
        event.setCapacity(request.getCapacity());
        event.setStartDate(request.getStartDate());
        event.setEndDate(request.getEndDate());
        event.setCity(request.getCity());
        event.setAddress(request.getAddress());
        event.setLatitude(request.getLatitude());
        event.setLongitude(request.getLongitude());
        event.setImageUrl(request.getImageUrl());
        event.setType(EventActivity.EventType
                .valueOf(request.getType()));
        event.setPromoType(resolvePromoType(request.getPromoType()));
        event.setPromoPercent(normalizePromoPercent(request.getPromoPercent()));
        event.setPromoCode(normalizePromoCode(request.getPromoCode()));
        event.setPromoStartDate(request.getPromoStartDate());
        event.setPromoEndDate(request.getPromoEndDate());
        event.setStatus(moderation.approved()
                ? EventActivity.EventStatus.PUBLISHED
                : EventActivity.EventStatus.REJECTED);
        event.setModeratedAt(LocalDateTime.now());
        event.setModeratedByEmail("qwen3-auto");
        event.setModerationReason(moderation.reason());

        EventCategory category = categoryRepository
                .findById(request.getCategoryId())
                .orElseThrow(() ->
                        new RuntimeException("Category not found"));
        event.setCategory(category);

        return toResponse(repository.save(event));
    }

    // ============================================================
    // PUBLISH — ADMIN publie (DRAFT → PUBLISHED)
    // ============================================================
        public EventActivityResponse publish(Integer id, String adminEmail) {
        EventActivity event = repository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("Event not found : " + id));

        // Seulement DRAFT peut être publié
        if (event.getStatus() != EventActivity.EventStatus.DRAFT) {
            throw new RuntimeException(
                    "Only DRAFT events can be published");
        }

        event.setStatus(EventActivity.EventStatus.PUBLISHED);
                event.setModeratedAt(java.time.LocalDateTime.now());
                event.setModeratedByEmail(adminEmail);
                event.setModerationReason(null);
        return toResponse(repository.save(event));
    }

    // ============================================================
    // REJECT — ADMIN rejette (DRAFT → REJECTED)
    // ============================================================
        public EventActivityResponse reject(Integer id, String adminEmail, String reason) {
        EventActivity event = repository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("Event not found : " + id));

        // Seulement DRAFT peut être rejeté
        if (event.getStatus() != EventActivity.EventStatus.DRAFT) {
            throw new RuntimeException(
                    "Only DRAFT events can be rejected");
        }

        event.setStatus(EventActivity.EventStatus.REJECTED);
                event.setModeratedAt(java.time.LocalDateTime.now());
                event.setModeratedByEmail(adminEmail);
                event.setModerationReason(
                                reason != null && !reason.trim().isEmpty() ? reason.trim() : null
                );
        return toResponse(repository.save(event));
    }

    // ============================================================
    // CANCEL — ORGANISATEUR ou ADMIN annule
    // + remboursement automatique
    // + email annulation envoyé à chaque client
    // ============================================================
    @Transactional
        public EventActivityResponse cancel(Integer id, String email, String cancellationReason) {
        EventActivity event = repository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("Event not found : " + id));

        // Seulement PUBLISHED peut être annulé
        if (event.getStatus() != EventActivity.EventStatus.PUBLISHED) {
            throw new RuntimeException(
                    "Only PUBLISHED events can be cancelled");
        }

        // Vérifier si ADMIN ou propriétaire de l'event
        User user = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new RuntimeException("User not found"));

        boolean isAdmin = user.getRole().name().equals("ADMIN");
        boolean isOwner = event.getOrganizer().getEmail().equals(email);

        if (!isAdmin && !isOwner) {
            throw new RuntimeException(
                    "Not authorized to cancel this event");
        }

                // Organisateur: motif obligatoire (minimum 10 caracteres)
                String normalizedReason = cancellationReason == null ? "" : cancellationReason.trim();
                if (!isAdmin && normalizedReason.length() < 10) {
                        throw new RuntimeException("Motif d'annulation obligatoire (minimum 10 caracteres)");
                }

                if (!normalizedReason.isEmpty()) {
                        log.info("Cancellation reason for event {} by {}: {}", event.getId(), email, normalizedReason);
                }

                event.setCancellationReason(normalizedReason.isEmpty() ? null : normalizedReason);

        // Traiter toutes les réservations
        List<EventReservation> reservations =
                reservationRepository.findByEventId(id);

        for (EventReservation reservation : reservations) {

            // 1. Rembourser payment
            paymentRepository
                    .findByReservationId(reservation.getId())
                    .ifPresent(payment -> {
                        if (payment.getPaymentStatus() ==
                                EventPayment.PaymentStatus.SUCCESS) {
                            payment.setPaymentStatus(
                                    EventPayment.PaymentStatus.REFUNDED);
                            paymentRepository.save(payment);
                        }
                    });

            // 2. Remettre les sièges
            event.setAvailableSeats(
                    event.getAvailableSeats() +
                            reservation.getNumberOfTickets());

            // 3. Annuler la réservation
            reservation.setStatus(
                    EventReservation.ReservationStatus.CANCELLED);
            reservationRepository.save(reservation);
            ticketService.cancelTicketsForReservation(reservation.getId());

            // 4. Email annulation
            try {
                java.time.format.DateTimeFormatter formatter = 
                    java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
                String formattedDate = event.getStartDate().format(formatter);
                
                emailService.sendEventCancellationEmail(
                        reservation.getUser().getEmail(),
                        reservation.getUser().getFullName(),
                        event.getTitle(),
                        formattedDate,
                        reservation.getTotalPrice()
                );
            } catch (Exception e) {
                log.warn("Email failed for user {}: {}",
                        reservation.getUser().getEmail(), e.getMessage());
            }

            // 5. SMS annulation
            try {
                java.time.format.DateTimeFormatter formatter = 
                    java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
                String formattedDate = event.getStartDate().format(formatter);
                String formattedAmount = reservation.getTotalPrice().toString();

                smsService.sendEventCancellationSms(
                        reservation.getUser().getPhone(),
                        reservation.getUser().getFullName(),
                        event.getTitle(),
                        formattedDate,
                        formattedAmount
                );
            } catch (Exception e) {
                log.warn("SMS failed for user {}: {}",
                        reservation.getUser().getEmail(), e.getMessage());
            }
        }

        // 5. Annuler l'event
        event.setStatus(EventActivity.EventStatus.CANCELLED);
        return toResponse(repository.save(event));
    }

    // ============================================================
    // DELETE — ADMIN supprime définitivement
    // ============================================================
    public void delete(Integer id) {
        repository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("Event not found : " + id));
        repository.deleteById(id);
    }

    // ============================================================
    // GET STATS — pour le dashboard admin
    // ============================================================
    public EventStatsResponse getStats() {
        long total = repository.count();
        long draft = repository.countByStatus(EventActivity.EventStatus.DRAFT);
        long published = repository.countByStatus(EventActivity.EventStatus.PUBLISHED);
        long rejected = repository.countByStatus(EventActivity.EventStatus.REJECTED);
        long cancelled = repository.countByStatus(EventActivity.EventStatus.CANCELLED);

        return EventStatsResponse.builder()
                .totalEvents(total)
                .draftCount(draft)
                .publishedCount(published)
                .rejectedCount(rejected)
                .cancelledCount(cancelled)
                .build();
    }

        public Map<String, Object> checkDateAvailability(LocalDate date) {
                LocalDateTime start = date.atStartOfDay();
                LocalDateTime end = date.plusDays(1).atStartOfDay();

                long eventsCount = repository.findByStartDateBetweenOrderByStartDateAsc(start, end)
                                .stream()
                                .filter(e -> e.getStatus() != EventActivity.EventStatus.CANCELLED)
                                .count();

                String status;
                String message;
                if (eventsCount <= 2) {
                        status = "AVAILABLE";
                        message = "Date disponible !";
                } else if (eventsCount <= 5) {
                        status = "BUSY";
                        message = eventsCount + " événements ce jour";
                } else {
                        status = "SATURATED";
                        message = "Date saturée";
                }

                List<String> suggestions = new ArrayList<>();
                if (eventsCount >= 3) {
                        for (int i = 1; i <= 45 && suggestions.size() < 3; i++) {
                                LocalDate candidate = date.plusDays(i);
                                LocalDateTime candidateStart = candidate.atStartOfDay();
                                LocalDateTime candidateEnd = candidate.plusDays(1).atStartOfDay();
                                long candidateCount = repository.findByStartDateBetweenOrderByStartDateAsc(candidateStart, candidateEnd)
                                                .stream()
                                                .filter(e -> e.getStatus() != EventActivity.EventStatus.CANCELLED)
                                                .count();
                                if (candidateCount <= 2) {
                                        suggestions.add(candidate.toString());
                                }
                        }
                }

                Map<String, Object> response = new LinkedHashMap<>();
                response.put("date", date.toString());
                response.put("eventsCount", eventsCount);
                response.put("status", status);
                response.put("message", message);
                response.put("suggestions", suggestions);
                return response;
        }

    // ============================================================
    // METHODE PRIVEE — Conversion entité → DTO Response
    // ============================================================
    private EventActivityResponse toResponse(EventActivity e) {
        return EventActivityResponse.builder()
                .id(e.getId())
                .title(e.getTitle())
                .description(e.getDescription())
                .price(e.getPrice())
                .capacity(e.getCapacity())
                .availableSeats(e.getAvailableSeats())
                .startDate(e.getStartDate())
                .endDate(e.getEndDate())
                .city(e.getCity())
                .address(e.getAddress())
                .latitude(e.getLatitude())
                .longitude(e.getLongitude())
                .imageUrl(e.getImageUrl())
                .type(e.getType() != null ?
                        e.getType().name() : null)
                .status(e.getStatus() != null ?
                        e.getStatus().name() : null)
                .createdAt(e.getCreatedAt())
                .updatedAt(e.getUpdatedAt())
                .moderatedAt(e.getModeratedAt())
                .moderatedByEmail(e.getModeratedByEmail())
                .moderationReason(e.getModerationReason())
                .cancellationReason(e.getCancellationReason())
                                .promoType(e.getPromoType() != null ? e.getPromoType().name() : EventActivity.PromoType.NONE.name())
                                .promoPercent(e.getPromoPercent())
                                .promoCode(e.getPromoCode())
                                .promoStartDate(e.getPromoStartDate())
                                .promoEndDate(e.getPromoEndDate())
                .categoryId(e.getCategory() != null ?
                        e.getCategory().getId() : null)
                .categoryName(e.getCategory() != null ?
                        e.getCategory().getName() : null)
                .organizerId(e.getOrganizer() != null ?
                        e.getOrganizer().getId() : null)
                .organizerName(e.getOrganizer() != null ?
                        e.getOrganizer().getFullName() : null)
                .build();
    }

        private EventActivity.PromoType resolvePromoType(String rawType) {
                if (rawType == null || rawType.trim().isEmpty()) {
                        return EventActivity.PromoType.NONE;
                }
                try {
                        return EventActivity.PromoType.valueOf(rawType.trim().toUpperCase());
                } catch (IllegalArgumentException ex) {
                        return EventActivity.PromoType.NONE;
                }
        }

        private Integer normalizePromoPercent(Integer percent) {
                if (percent == null) return null;
                int safe = Math.max(0, Math.min(80, percent));
                return safe == 0 ? null : safe;
        }

        private String normalizePromoCode(String code) {
                if (code == null) return null;
                String normalized = code.trim();
                return normalized.isEmpty() ? null : normalized;
        }

        private ModerationResult evaluateModeration(EventActivityRequest request) {
                EventModerationAiService.ModerationDecision aiDecision = moderationAiService.moderate(request);
                if (aiDecision != null) {
                        return new ModerationResult(aiDecision.approved(), aiDecision.score(), aiDecision.reason());
                }

                log.warn("AI moderation unavailable, using local fallback scoring");
                return evaluateModerationLocal(request);
        }

        private ModerationResult evaluateModerationLocal(EventActivityRequest request) {
                int score = 0;
                List<String> reasons = new ArrayList<>();

                int titleLen = realLength(request.getTitle());
                if (titleLen >= 15) {
                        score += 25;
                } else {
                        reasons.add("Titre trop court (< 15 caractères)");
                }

                int descLen = realLength(request.getDescription());
                if (descLen >= 80) {
                        score += 25;
                } else {
                        reasons.add("Description trop courte (< 80 caractères)");
                }

                if (!containsFakeTokens(request.getTitle(), request.getDescription(), request.getCity())) {
                        score += 20;
                } else {
                        reasons.add("Contenu factice détecté");
                }

                if (isLikelyTunisianCity(request.getCity())) {
                        score += 15;
                } else {
                        reasons.add("Ville non tunisienne ou imprécise");
                }

                if (isPriceCoherent(request.getPrice())) {
                        score += 15;
                } else {
                        reasons.add("Prix incohérent");
                }

                boolean approved = score >= 70;
                String reason = approved
                                ? "Validation auto Qwen3 (score " + score + "/100)"
                                : "Rejet auto Qwen3 (score " + score + "/100): " + String.join("; ", reasons);

                return new ModerationResult(approved, score, reason);
        }

        private int realLength(String value) {
                if (value == null) return 0;
                return value.replaceAll("\\s+", "")
                                .replaceAll("[^\\p{L}\\p{N}]", "")
                                .length();
        }

        private boolean containsFakeTokens(String... values) {
                String joined = Arrays.stream(values)
                                .filter(v -> v != null)
                                .collect(Collectors.joining(" "))
                                .toLowerCase();
                List<String> fakeTokens = List.of("test", "aaa", "xxx", "asdf", "qwerty", "lorem", "demo");
                return fakeTokens.stream().anyMatch(joined::contains);
        }

        private boolean isLikelyTunisianCity(String city) {
                if (city == null) return false;
                String normalized = city.trim().toLowerCase();
                Set<String> allowed = Set.of(
                                "tunis", "sfax", "sousse", "nabeul", "bizerte", "gabes", "kairouan",
                                "gafsa", "monastir", "mahdia", "tozeur", "kebili", "medenine", "djerba",
                                "hammamet", "tabarka", "zaghouan", "ariana", "ben arous", "manouba", "beja",
                                "jendouba", "siliana", "kasserine", "sidi bouzid", "kef", "tataouine", "douz",
                                "el jem", "carthage", "sidi bou said"
                );
                return allowed.stream().anyMatch(normalized::contains);
        }

        private boolean isPriceCoherent(java.math.BigDecimal price) {
                if (price == null) return false;
                return price.compareTo(java.math.BigDecimal.valueOf(5)) >= 0
                                && price.compareTo(java.math.BigDecimal.valueOf(2000)) <= 0;
        }

        private record ModerationResult(boolean approved, int score, String reason) {
        }
}