package tn.hypercloud.entity.transport;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import tn.hypercloud.entity.transport.enums.TypeVehicule;
import tn.hypercloud.entity.transport.enums.VehiculeStatut;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Entity
@Table(name = "vehicules")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Vehicule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_vehicule")
    private Long idVehicule;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_chauffeur", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private Chauffeur chauffeur;

    @Column(length = 100)
    private String marque;

    @Column(length = 100)
    private String modele;

    @Column(name = "numero_plaque", nullable = false, unique = true, length = 20)
    private String numeroPlaque;

    @Enumerated(EnumType.STRING)
    @Column(name = "type_vehicule", nullable = false)
    private TypeVehicule typeVehicule;

    @Column(name = "capacite_passagers")
    private Integer capacitePassagers;

    @Column(name = "prix_km", precision = 10, scale = 2)
    private BigDecimal prixKm;

    @Column(name = "prix_minute", precision = 10, scale = 2)
    private BigDecimal prixMinute;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private VehiculeStatut statut = VehiculeStatut.ACTIVE;

    @Column(name = "photo_urls", columnDefinition = "TEXT")
    private String photoUrlsSerialized;

    @Transient
    @Builder.Default
    private List<String> photoUrls = new ArrayList<>();

    @Column(updatable = false)
    private LocalDateTime dateCreation;

    private LocalDateTime dateModification;

    @PrePersist
    protected void onCreate() {
        syncPhotoUrlsToSerialized();
        dateCreation = LocalDateTime.now();
        dateModification = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        syncPhotoUrlsToSerialized();
        dateModification = LocalDateTime.now();
    }

    @PostLoad
    protected void onLoad() {
        syncSerializedToPhotoUrls();
    }

    private void syncPhotoUrlsToSerialized() {
        if (photoUrls == null || photoUrls.isEmpty()) {
            photoUrlsSerialized = null;
            return;
        }

        photoUrlsSerialized = photoUrls.stream()
                .filter(path -> path != null && !path.isBlank())
                .collect(Collectors.joining("||"));
    }

    private void syncSerializedToPhotoUrls() {
        if (photoUrlsSerialized == null || photoUrlsSerialized.isBlank()) {
            photoUrls = new ArrayList<>();
            return;
        }

        photoUrls = Arrays.stream(photoUrlsSerialized.split("\\\\|\\\\|"))
                .map(String::trim)
                .filter(path -> !path.isEmpty())
                .collect(Collectors.toCollection(ArrayList::new));
    }
}