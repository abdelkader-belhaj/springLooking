package tn.hypercloud.entity.event;

import jakarta.persistence.*;
import lombok.*;
import tn.hypercloud.entity.user.User;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "event_activity")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EventActivity {

    /**
     * Délai minimum avant {@link #startDate} : un organisateur ne peut annuler un événement
     * {@link EventStatus#PUBLISHED} au-delà de ce seuil. L'admin n'est pas soumis à cette limite.
     * Constante côté module event (pas de configuration globale partagée).
     */
    public static final int ORGANIZER_CANCEL_MIN_HOURS_BEFORE_START = 48;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Column(nullable = false)
    private int capacity;

    @Column(name = "available_seats", nullable = false)
    private int availableSeats;

    @Column(name = "start_date", nullable = false)
    private LocalDateTime startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDateTime endDate;

    @Column(nullable = false, length = 100)
    private String city;

    @Column(nullable = false, length = 255)
    private String address;

    private Double latitude;
    private Double longitude;

    @Column(name = "image_url", columnDefinition = "LONGTEXT")
    private String imageUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EventType type;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EventStatus status = EventStatus.DRAFT;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "moderated_at")
    private LocalDateTime moderatedAt;

    @Column(name = "moderated_by_email", length = 120)
    private String moderatedByEmail;

    @Column(name = "moderation_reason", columnDefinition = "TEXT")
    private String moderationReason;

    @Column(name = "cancellation_reason", columnDefinition = "TEXT")
    private String cancellationReason;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private EventCategory category;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organizer_id", nullable = false)
    private User organizer;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public enum EventType {
        EVENT,
        ACTIVITY
    }

    public enum EventStatus {
        // Event créé par l'organisateur, en attente de validation ADMIN
        DRAFT,

        // Event validé par l'ADMIN, visible et réservable par les clients
        PUBLISHED,

        // Event refusé par l'ADMIN, non visible aux clients
        REJECTED,

        // Event annulé après publication,
        // toutes les réservations annulées + remboursement automatique
        CANCELLED
    }
}
