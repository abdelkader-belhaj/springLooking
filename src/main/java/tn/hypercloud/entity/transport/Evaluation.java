package tn.hypercloud.entity.transport;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import tn.hypercloud.entity.transport.enums.EvaluationType;
import jakarta.persistence.*;
import lombok.*;
import tn.hypercloud.entity.user.User;

import java.time.LocalDateTime;

@Entity
@Table(name = "evaluations",
        uniqueConstraints = @UniqueConstraint(columnNames = {"id_course", "type"})  )
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Evaluation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_evaluation")
    private Long idEvaluation;

    // Une course peut avoir 2 évaluations : client→chauffeur et chauffeur→client
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_course", nullable = false)
    private Course course;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_evaluateur", nullable = false)
    private User evaluateur;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_evalue", nullable = false)
    private User evalue;

    // CLIENT_TO_DRIVER ou DRIVER_TO_CLIENT
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EvaluationType type;

    @Column(nullable = false)
    @Min(1)
    @Max(5)
    private Integer note;

    @Column(columnDefinition = "TEXT")
    private String commentaire;

    @Column(updatable = false)
    private LocalDateTime dateCreation;

    @PrePersist
    protected void onCreate() {
        dateCreation = LocalDateTime.now();
    }
}
