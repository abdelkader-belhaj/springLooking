package tn.hypercloud.entity.transport;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import tn.hypercloud.entity.transport.enums.ReservationStatus;
import tn.hypercloud.entity.user.User;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "reservations_location")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class ReservationLocation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idReservation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_client", nullable = false)
    @JsonIgnore
    private User client;

    @Transient
    private Long clientId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_vehicule_agence", nullable = false)
    @JsonIgnore
    private VehiculeAgence vehiculeAgence;

    @Transient
    private Long vehiculeAgenceId;

    private LocalDateTime dateDebut;
    private LocalDateTime dateFin;

    @Column(precision = 10, scale = 2)
    private BigDecimal prixTotal;

    @Column(precision = 10, scale = 2)
    private BigDecimal montantCommission;

    @Enumerated(EnumType.STRING)
    private ReservationStatus statut = ReservationStatus.PENDING;

    @Column(columnDefinition = "TEXT")
    private String commentaire;

    @Column(updatable = false)
    private LocalDateTime dateCreation;

    private LocalDateTime dateModification;

    @PrePersist
    protected void onCreate() {
        dateCreation = LocalDateTime.now();
        dateModification = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        dateModification = LocalDateTime.now();
    }
}