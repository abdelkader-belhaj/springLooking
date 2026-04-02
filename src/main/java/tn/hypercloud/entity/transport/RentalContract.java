package tn.hypercloud.entity.transport;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "rental_contracts")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class RentalContract {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "id_reservation", nullable = false)
    private ReservationLocation reservationLocation;

    @Column(length = 500)
    private String contractPdfUrl;          // chemin du PDF final

    @Column(length = 500)
    private String signatureImageUrl;       // image signature (base64 → fichier)

    private LocalDateTime dateSignature;

    @Column(length = 100)
    private String signedBy;                // nom du client qui a signé

    private LocalDateTime dateCreation;
    private LocalDateTime dateModification;

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