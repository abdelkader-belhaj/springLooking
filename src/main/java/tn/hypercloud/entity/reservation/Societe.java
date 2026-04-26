package tn.hypercloud.entity.reservation;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "societe")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Societe {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, length = 150)
    private String nom;

    @Column(length = 255)
    private String logoUrl;

    @Column(length = 100)
    private String pays;
    @Column(name = "user_id", unique = true)
    private Long userId;
}
