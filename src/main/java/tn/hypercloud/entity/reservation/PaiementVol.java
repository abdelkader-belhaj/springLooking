package tn.hypercloud.entity.reservation;



import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "paiement_vol")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PaiementVol {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    /** 1 seul paiement par réservation */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_reservation", nullable = false, unique = true)
    private ReservationVol reservation;

    /** ex: 'carte', 'paypal', 'flouci' */
    @Column(nullable = false, length = 50)
    private String methode;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal montant;

    /** ID retourné par Stripe, Flouci, etc. */
    @Column(name = "reference_tx", length = 100)
    private String referenceTx;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private StatutPaiement statut = StatutPaiement.en_attente;

    /** Renseigné quand statut = 'paye' */
    @Column(name = "date_paiement")
    private LocalDateTime datePaiement;

    @Column(name = "date_creation", updatable = false)
    private LocalDateTime dateCreation;

    @PrePersist
    protected void onCreate() {
        dateCreation = LocalDateTime.now();
    }

    public enum StatutPaiement { en_attente, paye, echec }
}
