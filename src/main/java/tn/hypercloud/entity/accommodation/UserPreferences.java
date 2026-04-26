package tn.hypercloud.entity.accommodation;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "user_preferences")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class UserPreferences {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, unique = true)
    private Integer userId;

    // Budget préféré par nuit (DT)
    @Column(name = "budget_max")
    private Double budgetMax;

    // Type de séjour préféré (categorie nom)
    @Column(name = "type_sejour", length = 100)
    private String typeSejour;

    // Ville préférée
    @Column(name = "ville_preferee", length = 100)
    private String villePreferee;

    // Capacité minimale souhaitée
    @Column(name = "capacite_min")
    private Integer capaciteMin;

    // Équipements souhaités (liste séparée par virgules)
    @Column(name = "equipements_souhaites", columnDefinition = "TEXT")
    private String equipementsSouhaites;

    // Ambiance souhaitée: "calme", "animé", "nature", "ville"
    @Column(name = "ambiance", length = 50)
    private String ambiance;
}
