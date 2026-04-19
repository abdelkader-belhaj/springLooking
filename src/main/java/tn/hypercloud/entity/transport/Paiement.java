package tn.hypercloud.entity.transport;

import tn.hypercloud.entity.transport.enums.PaiementStatut;
import tn.hypercloud.entity.transport.enums.PaiementMethode;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "paiements")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Paiement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_paiement")
    private Long idPaiement;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_course", nullable = false, unique = true)
    private Course course;

    @Column(precision = 10, scale = 2)
    private BigDecimal montant;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaiementMethode methode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private PaiementStatut statut = PaiementStatut.PENDING;

    private LocalDateTime datePaiement;

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
