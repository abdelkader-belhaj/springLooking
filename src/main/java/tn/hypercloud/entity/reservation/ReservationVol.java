package tn.hypercloud.entity.reservation;

import jakarta.persistence.*;
import lombok.*;
import tn.hypercloud.entity.user.User;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "reservation_vol")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ReservationVol {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_touriste", nullable = false)
    private User touriste;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_vol_aller", nullable = false)
    private Vol volAller;

    /** NULL = aller simple */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_vol_retour")
    private Vol volRetour;

    @Enumerated(EnumType.STRING)
    @Column(name = "type_billet", nullable = false)
    private Panier.TypeBillet typeBillet;

    @Column(name = "nb_passagers", nullable = false)
    private byte nbPassagers = 1;

    @Column(name = "prix_total", nullable = false, precision = 10, scale = 2)
    private BigDecimal prixTotal;

    /** Référence unique ex: TUN1234 */
    @Column(nullable = false, unique = true, length = 10)
    private String reference;

    @Column(name = "date_reservation", updatable = false)
    private LocalDateTime dateReservation;

    @OneToOne(mappedBy = "reservation", cascade = CascadeType.ALL)
    private PaiementVol paiement;

    @PrePersist
    protected void onCreate() {
        dateReservation = LocalDateTime.now();
    }
}
