package tn.hypercloud.entity.transport;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import tn.hypercloud.entity.user.User;
import tn.hypercloud.entity.transport.enums.EvaluationType;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "evaluations",
        uniqueConstraints = @UniqueConstraint(columnNames = {"id_course", "type"})  )
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
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
    @JsonIgnore
    private User evaluateur;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_evalue", nullable = false)
    @JsonIgnore
    private User evalue;
    @Transient
    private Long evaluateurId;

    @Transient
    private Long evalueId;
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
