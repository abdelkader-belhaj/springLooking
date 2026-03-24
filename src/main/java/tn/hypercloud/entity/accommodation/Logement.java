package tn.hypercloud.entity.accommodation;


import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "logement")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Logement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_logement")
    private Integer idLogement;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_categorie", nullable = false)
    private Categorie categorie;

    @Column(nullable = false, length = 150)
    private String nom;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    @Column(name = "video_url", length = 500)
    private String videoUrl;

    @Column(length = 255)
    private String adresse;

    @Column(length = 100)
    private String ville;

    @Column(name = "prix_nuit", nullable = false, precision = 10, scale = 2)
    private BigDecimal prixNuit;

    @Column(nullable = false)
    private int capacite = 1;

    @Column(nullable = false)
    private boolean disponible = true;

    @Column(name = "date_creation", updatable = false)
    private LocalDateTime dateCreation;

    @PrePersist
    protected void onCreate() {
        dateCreation = LocalDateTime.now();
    }
}
