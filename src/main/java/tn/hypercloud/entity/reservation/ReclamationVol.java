package tn.hypercloud.entity.reservation;

import jakarta.persistence.*;
import lombok.*;
import tn.hypercloud.entity.user.User;

import java.time.LocalDateTime;

@Entity
@Table(name = "reclamation_vol")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReclamationVol {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_touriste", nullable = false)
    private User touriste;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_reservation", nullable = false)
    private ReservationVol reservation;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Priorite priorite;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String sujet;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private Statut statut = Statut.ouverte;

    @Column(columnDefinition = "TEXT")
    private String reponse;

    private LocalDateTime dateReponse;

    @Column(nullable = false, updatable = false)
    private LocalDateTime dateCreation;

    /**
     * Notification "in-site": quand une réponse existe et que le client ne l’a pas encore vue,
     * on met clientLu=false. Dès qu’il ouvre/consulte, on passe à true.
     */
    @Column(nullable = false)
    @Builder.Default
    private Boolean clientLu = false;

    @PrePersist
    protected void onCreate() {
        if (dateCreation == null) {
            dateCreation = LocalDateTime.now();
        }
        if (statut == null) {
            statut = Statut.ouverte;
        }
        if (clientLu == null) {
            clientLu = false;
        }
    }

    public enum Priorite {
        urgent,
        normale,
        tres_urgent
    }

    public enum Statut {
        ouverte,
        repondue
    }
}

