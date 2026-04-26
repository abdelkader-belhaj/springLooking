package tn.hypercloud.entity.accommodation;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.List;
import tn.hypercloud.entity.user.User;

@Entity
@Table(name = "categorie")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Categorie {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_categorie")
    private Integer idCategorie;

    @Column(name = "nom_categorie", nullable = false, length = 100)
    private String nomCategorie;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(length = 255)
    private String icone;

    @Column(nullable = false)
    private boolean statut = true;

    @Column(name = "date_creation", updatable = false)
    private LocalDateTime dateCreation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id")
    private User owner;

    @OneToMany(mappedBy = "categorie")
    private List<Logement> logements;

    @PrePersist
    protected void onCreate() {
        dateCreation = LocalDateTime.now();
    }
}