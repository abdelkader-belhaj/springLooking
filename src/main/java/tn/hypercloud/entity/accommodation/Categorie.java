package tn.hypercloud.entity.accommodation;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "categorie")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Categorie {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_categorie")
    private Integer idCategorie;

    @Column(name = "nom_categorie", nullable = false, unique = true, length = 100)
    private String nomCategorie;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(length = 255)
    private String icone;

    @Column(nullable = false)
    private boolean statut = true;

    @Column(name = "date_creation", updatable = false)
    private LocalDateTime dateCreation;

    @PrePersist
    protected void onCreate() {
        dateCreation = LocalDateTime.now();
    }
}
