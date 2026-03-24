package tn.hypercloud.entity.reservation;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Table(name = "vol")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Vol {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_societe", nullable = false)
    private Societe societe;

    @Column(nullable = false, length = 20)
    private String numero;

    @Column(nullable = false, length = 10)
    private String depart;

    @Column(nullable = false, length = 10)
    private String arrivee;

    @Column(name = "date_depart", nullable = false)
    private LocalDate dateDepart;

    @Column(name = "heure_depart", nullable = false)
    private LocalTime heureDepart;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal prix;

    @Column(nullable = false)
    private int places = 0;
}
