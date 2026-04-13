package tn.hypercloud.entity.transport;

import com.fasterxml.jackson.annotation.JsonIgnore;
import tn.hypercloud.entity.transport.enums.AnnulePar;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "annulationTransport")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AnnulationTransport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_annulation")
    private Long idAnnulation;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_course", nullable = false, unique = true)
    @JsonIgnore
    private Course course;

    @Enumerated(EnumType.STRING)
    @Column(name = "annule_par", nullable = false)
    private AnnulePar annulePar;

    @Column(length = 500)
    private String raison;

    @Column(name = "montant_penalite", precision = 10, scale = 2)
    private BigDecimal montantPenalite;

    @Column(name = "montant_remboursement", precision = 10, scale = 2)
    private BigDecimal montantRemboursement;

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
