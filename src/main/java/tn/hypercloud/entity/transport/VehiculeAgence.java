package tn.hypercloud.entity.transport;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import tn.hypercloud.entity.transport.enums.TypeVehicule;
import tn.hypercloud.entity.transport.enums.VehiculeStatut;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "vehicules_agence")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class VehiculeAgence {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_vehicule_agence")
    private Long idVehiculeAgence;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_agence", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private AgenceLocation agence;

    @Transient
    private Long agenceId;

    @Column(length = 100)
    private String marque;

    @Column(length = 100)
    private String modele;

    @Column(name = "numero_plaque", nullable = false, unique = true, length = 20)
    private String numeroPlaque;

    @Enumerated(EnumType.STRING)
    @Column(name = "type_vehicule", nullable = false)
    private TypeVehicule typeVehicule;

    @Column(name = "capacite_passagers")
    private Integer capacitePassagers;

    @Column(name = "prix_km", precision = 10, scale = 2)
    private BigDecimal prixKm;

    @Column(name = "prix_minute", precision = 10, scale = 2)
    private BigDecimal prixMinute;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private VehiculeStatut statut = VehiculeStatut.ACTIVE;

    @OneToMany(mappedBy = "vehiculeAgence", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ReservationLocation> reservations = new ArrayList<>();
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