package tn.hypercloud.entity.accommodation;


import jakarta.persistence.*;
import lombok.*;
import tn.hypercloud.entity.user.User;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "reservation")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ReservationLogement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_reservation")
    private Integer idReservation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_logement", nullable = false)
    private Logement logement;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_client", nullable = false)
    private User client;

    @Column(name = "date_debut", nullable = false)
    private LocalDate dateDebut;

    @Column(name = "date_fin", nullable = false)
    private LocalDate dateFin;

    @Column(name = "nb_personnes", nullable = false)
    @Builder.Default
    private int nbPersonnes = 1;

    @Column(name = "prix_total", precision = 10, scale = 2)
    private BigDecimal prixTotal;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private StatutReservation statut = StatutReservation.en_attente;

    @Column(name = "date_reservation", updatable = false)
    private LocalDateTime dateReservation;

    @PrePersist
    protected void onCreate() {
        dateReservation = LocalDateTime.now();
    }

    public enum StatutReservation {
        en_attente,
        confirmee,
        annulee,
        terminee
    }
}
