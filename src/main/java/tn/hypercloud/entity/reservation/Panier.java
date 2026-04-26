package tn.hypercloud.entity.reservation;

import jakarta.persistence.*;
import lombok.*;
import tn.hypercloud.entity.user.User;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "panier")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Panier {

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
    private TypeBillet typeBillet;

    @Column(name = "nb_passagers", nullable = false)
    private byte nbPassagers = 1;

    @Column(name = "prix_total", nullable = false, precision = 10, scale = 2)
    private BigDecimal prixTotal;

    @Column(name = "date_ajout", updatable = false)
    private LocalDateTime dateAjout;

    @PrePersist
    protected void onCreate() {
        dateAjout = LocalDateTime.now();
    }

    public enum TypeBillet { aller_simple, aller_retour }
}
