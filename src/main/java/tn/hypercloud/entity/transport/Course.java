package tn.hypercloud.entity.transport;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import tn.hypercloud.entity.transport.enums.CourseStatus;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "courses")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Course {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_course")
    private Long idCourse;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_demande", nullable = false, unique = true)
    @JsonIgnore
    private DemandeCourse demande;

    // Lien Matching → traçabilité complète du flow
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_matching",  unique = true)
    @JsonIgnore
    private Matching matching;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_chauffeur", nullable = false)
    private Chauffeur chauffeur;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_vehicule", nullable = false)
    private Vehicule vehicule;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_localisation_depart", nullable = false)
    private Localisation localisationDepart;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_localisation_arrivee", nullable = false)
    private Localisation localisationArrivee;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private CourseStatus statut = CourseStatus.ACCEPTED;  // default = ACCEPTED

    @Column(name = "prix_final", precision = 10, scale = 2)
    private BigDecimal prixFinal;

    @Column(precision = 10, scale = 2)
    private BigDecimal montantCommission;

    @Column(name = "payment_client_confirmed", nullable = false)
    @Builder.Default
    private boolean paymentClientConfirmed = false;

    @Column(name = "payment_verified_by_driver", nullable = false)
    @Builder.Default
    private boolean paymentVerifiedByDriver = false;

    @Column(name = "payment_verification_code", length = 12)
    private String paymentVerificationCode;

    @Column(name = "payment_intent_id", length = 128)
    private String paymentIntentId;

    private LocalDateTime paymentClientConfirmedAt;

    private LocalDateTime paymentVerifiedAt;

    @Column(updatable = false)
    private LocalDateTime dateCreation;

    private LocalDateTime dateModification;

    @OneToOne(mappedBy = "course", cascade = CascadeType.ALL)
    private PaiementTransport paiementTransport;

    @OneToMany(mappedBy = "course", cascade = CascadeType.ALL)
    @JsonIgnore
    private List<EvaluationTransport> evaluationTransports;

    @OneToOne(mappedBy = "course", cascade = CascadeType.ALL)
    private AnnulationTransport annulationTransport;

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
