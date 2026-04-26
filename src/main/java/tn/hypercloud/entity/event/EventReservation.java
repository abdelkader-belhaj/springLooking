package tn.hypercloud.entity.event;

import jakarta.persistence.*;
import lombok.*;
import tn.hypercloud.entity.user.User;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "event_reservation")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class EventReservation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "reservation_date", nullable = false, updatable = false)
    private LocalDateTime reservationDate;

    @Column(name = "number_of_tickets", nullable = false)
    @Builder.Default
    private int numberOfTickets = 1;

    @Column(name = "total_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalPrice;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private ReservationStatus status = ReservationStatus.PENDING;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    private EventActivity event;

    // à ajouter Dans EventReservation.java
    @Column(name = "qr_used", nullable = false)
    @Builder.Default
    private boolean qrUsed = false;
    @Column(name = "qr_used_at")
    private LocalDateTime qrUsedAt;

    @Column(name = "qr_used_by")
    private String qrUsedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @OneToOne(mappedBy = "reservation", cascade = CascadeType.ALL)
    private EventPayment payment;

    @OneToMany(mappedBy = "reservation", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<EventTicket> tickets = new ArrayList<>();


    @PrePersist
    protected void onCreate() {
        reservationDate = LocalDateTime.now();
    }

    public enum ReservationStatus {
        // Client a réservé mais n'a pas encore payé
        PENDING,

        // Paiement effectué avec succès,
        // réservation confirmée automatiquement
        CONFIRMED,

        // Réservation annulée par le client ou
        // suite à l'annulation de l'event par l'admin/organisateur
        CANCELLED
    }}
