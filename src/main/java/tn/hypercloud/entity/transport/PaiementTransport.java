package tn.hypercloud.entity.transport;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;
import tn.hypercloud.entity.transport.enums.PaiementMethode;
import tn.hypercloud.entity.transport.enums.PaiementReservationPhase;
import tn.hypercloud.entity.transport.enums.PaiementStatut;
import tn.hypercloud.entity.transport.enums.PaiementType;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

@Entity
@Table(name = "paiementtransport")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class PaiementTransport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_paiement")
    private Long idPaiement;

    // Supporte à la fois Course ET ReservationLocation
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_course", nullable = true, unique = true)
    @JsonIgnore
    private Course course;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_reservation_location", nullable = true)
    @JsonIgnore
    private ReservationLocation reservationLocation;

    @Enumerated(EnumType.STRING)
    @Column(name = "phase_paiement", length = 20)
    private PaiementReservationPhase phasePaiement;

    @Column(precision = 10, scale = 2, nullable = false)
    private BigDecimal montantTotal;      // Payé par le client

    @Column(precision = 10, scale = 2, nullable = false)
    private BigDecimal montantCommission; // Revenu plateforme

    @Column(precision = 10, scale = 2, nullable = false)
    private BigDecimal montantNet;        // Revenu chauffeur / agence

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaiementMethode methode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private PaiementStatut statut = PaiementStatut.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(name = "type_paiement", nullable = false)
    private PaiementType typePaiement;   // ← nouveau

    private LocalDateTime datePaiement;

    @Column(updatable = false)
    private LocalDateTime dateCreation;

    private LocalDateTime dateModification;

    /** Calcul automatique de la commission */
    public void calculerCommission() {
        if (montantTotal == null) return;

        BigDecimal taux = (typePaiement == PaiementType.COURSE)
                ? new BigDecimal("0.20")
                : new BigDecimal("0.10");

        this.montantCommission = montantTotal
                .multiply(taux)
                .setScale(2, RoundingMode.HALF_UP);

        this.montantNet = montantTotal.subtract(this.montantCommission);
    }

    @PrePersist
    protected void onCreate() {
        dateCreation = LocalDateTime.now();
        dateModification = LocalDateTime.now();

        // Détection automatique du type
        if (course != null) {
            this.typePaiement = PaiementType.COURSE;
        } else if (reservationLocation != null) {
            this.typePaiement = PaiementType.RESERVATION_LOCATION;
        }

        // Calcul automatique
        if (montantCommission == null && montantTotal != null) {
            calculerCommission();
        }
    }

    protected void onUpdate() {
        dateModification = LocalDateTime.now();
    }

    /** Validation : exactement un seul lien */
    @PreUpdate
    private void validateLinks() {
        if ((course != null && reservationLocation != null) ||
                (course == null && reservationLocation == null)) {
            throw new IllegalStateException("Un paiement doit être lié à EXACTEMENT une Course OU une ReservationLocation");
        }
    }
}