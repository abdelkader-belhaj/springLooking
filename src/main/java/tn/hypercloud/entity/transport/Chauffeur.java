package tn.hypercloud.entity.transport;
import tn.hypercloud.entity.transport.enums.ChauffeurStatut;
import tn.hypercloud.entity.transport.enums.DisponibiliteStatut;
import jakarta.persistence.*;
import lombok.*;
import tn.hypercloud.entity.user.User;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "chauffeurs")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Chauffeur {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_chauffeur")
    private Long idChauffeur;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_utilisateur", nullable = false, unique = true)
    private User utilisateur;

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
