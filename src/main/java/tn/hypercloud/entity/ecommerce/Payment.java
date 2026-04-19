package tn.hypercloud.entity.ecommerce;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entité représentant un paiement Stripe associé à une commande.
 *
 * Workflow:
 *  1. INSERT payment (status=pending) avant appel Stripe
 *  2. UPDATE stripe_payment_intent_id après création du PaymentIntent
 *  3. UPDATE status + paid_at via Stripe Webhooks
 */
@Entity
@Table(
    name = "payments",
    indexes = {
        @Index(name = "idx_order_id",                columnList = "order_id"),
        @Index(name = "idx_stripe_payment_intent",   columnList = "stripe_payment_intent_id"),
        @Index(name = "idx_status",                  columnList = "status"),
        @Index(name = "idx_created_at",              columnList = "created_at"),
        @Index(name = "idx_paid_at",                 columnList = "paid_at")
    }
)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Commande associée à ce paiement */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    /** ID du PaymentIntent Stripe (ex: pi_3Nxxx...) */
    @Column(name = "stripe_payment_intent_id", unique = true, length = 255)
    private String stripePaymentIntentId;

    /** Montant du paiement */
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    /** Statut du paiement */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus status = PaymentStatus.pending;

    /** Horodatage de création (initiation du paiement) */
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /** Horodatage du succès du paiement — NULL si pas encore payé */
    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public enum PaymentStatus {
        pending,
        succeeded,
        failed,
        refunded
    }
}
