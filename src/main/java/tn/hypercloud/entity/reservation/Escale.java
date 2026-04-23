package tn.hypercloud.entity.reservation;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "escale")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Escale {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false)
    private String ville;

    @Column(nullable = false)
    private String duree; // e.g., "2h 30m"

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_vol", nullable = false)
    private Vol vol;
}
