package tn.hypercloud.entity.transport;

import jakarta.persistence.*;
import lombok.*;
import tn.hypercloud.entity.transport.enums.AnnulationPar;
import tn.hypercloud.entity.transport.enums.PhaseAnnulation;
import tn.hypercloud.entity.transport.enums.StatutRemboursement;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "annulations_location")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AnnulationLocation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_reservation", nullable = false)
    private ReservationLocation reservation;

    @Enumerated(EnumType.STRING)
    @Column(name = "annule_par", length = 20, nullable = false)
    private AnnulationPar annulePar;

    @Enumerated(EnumType.STRING)
    @Column(name = "phase_annulation", length = 30, nullable = false)
    private PhaseAnnulation phaseAnnulation;

    @Column(name = "montant_rembourse", precision = 10, scale = 2)
    private BigDecimal montantRembourse;

    @Column(name = "montant_perdu", precision = 10, scale = 2)
    private BigDecimal montantPerdu;

    @Column(columnDefinition = "TEXT")
    private String raison;

    @Enumerated(EnumType.STRING)
    @Column(name = "statut_remboursement", length = 20, nullable = false)
    private StatutRemboursement statutRemboursement;

    @Column(name = "date_creation", updatable = false)
    private LocalDateTime dateCreation;

    @PrePersist
    protected void onCreate() {
        dateCreation = LocalDateTime.now();
    }
}
