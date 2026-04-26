package tn.hypercloud.entity.reservation;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "qrcode_vol")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class QrCodeVol {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    // Lien 1-1 avec la réservation
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_reservation", nullable = false, unique = true)
    private ReservationVol reservation;

    // Le contenu encodé dans le QR (JSON ou texte structuré)
    @Column(name = "contenu", nullable = false, columnDefinition = "TEXT")
    private String contenu;

    // Image QR code encodée en Base64 (PNG)
    @Column(name = "image_base64", nullable = false, columnDefinition = "MEDIUMTEXT")
    private String imageBase64;

    @Column(name = "date_generation", updatable = false)
    private LocalDateTime dateGeneration;

    @PrePersist
    protected void onCreate() {
        dateGeneration = LocalDateTime.now();
    }
}