package tn.hypercloud.entity.transport;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import tn.hypercloud.entity.user.User;
import tn.hypercloud.entity.transport.enums.ChauffeurStatut;
import tn.hypercloud.entity.transport.enums.DisponibiliteStatut;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import jakarta.persistence.Transient;
@Entity
@Table(name = "chauffeurs")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Chauffeur {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_chauffeur")
    private Long idChauffeur;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_utilisateur", nullable = false, unique = true)
    @JsonIgnore
    private User utilisateur;
    @Transient
    private Long utilisateurId;

    @Transient
    private String nomAffichage;

    @Transient
    private String emailAffichage;

    @Transient
    private String photoProfil;
    @Column(nullable = false, length = 20)
    private String telephone;

    @Column(name = "numero_licence", nullable = false, unique = true, length = 50)
    private String numeroLicence;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private ChauffeurStatut statut = ChauffeurStatut.PENDING_VERIFICATION;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private DisponibiliteStatut disponibilite = DisponibiliteStatut.UNAVAILABLE;

    @Column(name = "note_moyenne", precision = 3, scale = 2)
    private BigDecimal noteMoyenne;

    @Column(name = "solde", precision = 10, scale = 2, nullable = false, columnDefinition = "DECIMAL(10,2) DEFAULT 0.00")
    @Builder.Default
    private BigDecimal solde = BigDecimal.ZERO;

    @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "id_position_actuelle", nullable = true)
    private Localisation positionActuelle;

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