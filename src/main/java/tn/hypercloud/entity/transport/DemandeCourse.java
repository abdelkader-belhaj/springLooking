package tn.hypercloud.entity.transport;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import tn.hypercloud.entity.user.User;
import tn.hypercloud.entity.transport.enums.ConfirmationClientStatut;
import tn.hypercloud.entity.transport.enums.DemandeStatus;
import tn.hypercloud.entity.transport.enums.TypeVehicule;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "demandes_courses")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})

public class DemandeCourse {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_demande")
    private Long idDemande;

    @ManyToOne
            (fetch = FetchType.LAZY)
    @JoinColumn(name = "id_client", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private User client;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_localisation_depart", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private Localisation localisationDepart;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_localisation_arrivee", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private Localisation localisationArrivee;

    @Enumerated(EnumType.STRING)
    @Column(name = "type_vehicule_demande", nullable = false)
    private TypeVehicule typeVehiculeDemande;

    @Column(name = "prix_estime", precision = 10, scale = 2)
    private BigDecimal prixEstime;

    @Column(name = "approbation_client_requise")
    private Boolean approbationClientRequise;

    @Column(name = "prix_client_accepte")
    private Boolean prixClientAccepte;

    @Column(name = "prix_propose", precision = 38, scale = 2)
    private BigDecimal prixPropose;

    @Enumerated(EnumType.STRING)
    @Column(name = "confirmation_client_statut", nullable = false)
    @Builder.Default
    private ConfirmationClientStatut confirmationClientStatut = ConfirmationClientStatut.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private DemandeStatus statut = DemandeStatus.PENDING;

    @Column(updatable = false)
    private LocalDateTime dateCreation;

    private LocalDateTime dateModification;

    @OneToOne(mappedBy = "demande", fetch = FetchType.LAZY)
    @JsonIgnoreProperties("demande")
    private Course course;

    @PrePersist
    protected void onCreate() {
        dateCreation = LocalDateTime.now();
        dateModification = LocalDateTime.now();
        if (approbationClientRequise == null) {
            approbationClientRequise = Boolean.TRUE;
        }
        if (prixClientAccepte == null) {
            prixClientAccepte = Boolean.FALSE;
        }
        if (confirmationClientStatut == null) {
            confirmationClientStatut = ConfirmationClientStatut.PENDING;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        dateModification = LocalDateTime.now();
    }
}

