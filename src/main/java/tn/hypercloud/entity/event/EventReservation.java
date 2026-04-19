package tn.hypercloud.entity.event;

import jakarta.persistence.*;
import lombok.*;
import tn.hypercloud.entity.user.User;

import java.math.BigDecimal;
import java.time.LocalDateTime;

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
    private int numberOfTickets = 1;

    @Column(name = "total_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalPrice;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReservationStatus status = ReservationStatus.PENDING;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    private EventActivity event;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @OneToOne(mappedBy = "reservation", cascade = CascadeType.ALL)
    private EventPayment payment;

    @PrePersist
    protected void onCreate() {
        reservationDate = LocalDateTime.now();
    }

    public enum ReservationStatus { PENDING, CONFIRMED, CANCELLED }
}
