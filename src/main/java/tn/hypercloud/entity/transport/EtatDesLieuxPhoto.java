package tn.hypercloud.entity.transport;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "etat_des_lieux_photos")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class EtatDesLieuxPhoto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "id_reservation")
    private ReservationLocation reservationLocation;

    @Column(columnDefinition = "TEXT")
    private String photoUrl;

    @Column(length = 50)
    private String type; // "CHECK_OUT" ou "CHECK_IN"

    private LocalDateTime dateUpload;
}