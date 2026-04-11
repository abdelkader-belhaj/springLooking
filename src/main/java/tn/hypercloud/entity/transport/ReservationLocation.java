package tn.hypercloud.entity.transport;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;
import tn.hypercloud.entity.transport.enums.DepositStatus;
import tn.hypercloud.entity.transport.enums.LicenseStatus;
import tn.hypercloud.entity.transport.enums.ReservationStatus;
import tn.hypercloud.entity.transport.enums.TypeVehicule;
import tn.hypercloud.entity.user.User;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import tn.hypercloud.config.LenientLocalDateTimeDeserializer;

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
    @Column(name = "id_reservation")
    private Long idReservation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_client", nullable = false)
    private User client;

    // Transient pour le frontend (comme dans tes autres entités)
    @Transient
    private Long clientId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_vehicule_agence", nullable = false)
    private VehiculeAgence vehiculeAgence;

    @Transient
    private Long vehiculeAgenceId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_agence", nullable = true)
    private AgenceLocation agenceLocation;

    @JsonDeserialize(using = LenientLocalDateTimeDeserializer.class)
    private LocalDateTime dateDebut;

    @JsonDeserialize(using = LenientLocalDateTimeDeserializer.class)
    private LocalDateTime dateFin;

    @Enumerated(EnumType.STRING)
    private TypeVehicule typeVehiculeDemande;

    @Column(precision = 10, scale = 2, nullable = false)
    private BigDecimal prixTotal;
    @Column(columnDefinition = "TEXT")
    private String note;
    @Column(precision = 10, scale = 2)
    private BigDecimal depositAmount;
    @Column(precision = 10, scale = 2)
    private BigDecimal montantCommission;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private DepositStatus depositStatus = DepositStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private ReservationStatus statut = ReservationStatus.DRAFT;

    @Column(length = 100)
    private String prenom;

    @Column(length = 100)
    private String nom;

    private LocalDateTime dateNaiss;

    @Column(length = 50)
    private String numeroPermis;

    private LocalDateTime licenseExpiryDate;

    @Column(length = 500)
    private String licenseImageUrl;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private LicenseStatus licenseStatus = LicenseStatus.PENDING;

    @OneToOne(mappedBy = "reservationLocation", cascade = CascadeType.ALL, orphanRemoval = true)
    private RentalContract rentalContract;

    @OneToMany(mappedBy = "reservationLocation", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<EtatDesLieuxPhoto> etatDesLieuxPhotos = new ArrayList<>();

    @OneToOne(mappedBy = "reservationLocation", cascade = CascadeType.ALL)
    private PaiementTransport paiementTransport;

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

    public boolean isOverlapping(LocalDateTime newStart, LocalDateTime newEnd) {
        return !(dateFin.isBefore(newStart) || dateDebut.isAfter(newEnd));
    }
}