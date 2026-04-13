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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Entity
@Table(name = "vehicules")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Vehicule {

    private static final Pattern PHOTO_PATH_PATTERN = Pattern.compile(
            "vehicules/\\d+/[^|\\s]+?\\.(?:png|jpe?g|webp|gif)",
            Pattern.CASE_INSENSITIVE
    );

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
    private VehiculeStatut statut = VehiculeStatut.INACTIVE;

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
        photoUrls = normalizePhotoUrls(photoUrls, photoUrlsSerialized);

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

        photoUrls = normalizePhotoUrls(null, photoUrlsSerialized);
    }

    private List<String> normalizePhotoUrls(List<String> listValue, String serializedValue) {
        List<String> candidates = new ArrayList<>();

        if (listValue != null) {
            candidates.addAll(listValue.stream()
                    .filter(path -> path != null && !path.isBlank())
                    .map(String::trim)
                    .collect(Collectors.toList()));
        }

        if (candidates.stream().anyMatch(path -> path.contains("/"))) {
            return deduplicate(candidates);
        }

        StringBuilder rawBuilder = new StringBuilder();
        if (!candidates.isEmpty()) {
            rawBuilder.append(String.join("", candidates));
        }
        if (serializedValue != null && !serializedValue.isBlank()) {
            if (rawBuilder.length() > 0) {
                rawBuilder.append("||");
            }
            rawBuilder.append(serializedValue);
        }

        String raw = rawBuilder.toString();
        if (raw.isBlank()) {
            return new ArrayList<>();
        }

        List<String> extracted = Arrays.stream(raw.split("\\|\\|"))
                .map(String::trim)
                .filter(path -> !path.isEmpty() && path.contains("/") && path.contains("."))
                .collect(Collectors.toCollection(ArrayList::new));

        Matcher matcher = PHOTO_PATH_PATTERN.matcher(raw);
        while (matcher.find()) {
            extracted.add(matcher.group());
        }

        String compacted = raw.replace("||", "");
        Matcher compactMatcher = PHOTO_PATH_PATTERN.matcher(compacted);
        while (compactMatcher.find()) {
            extracted.add(compactMatcher.group());
        }

        return deduplicate(extracted);
    }

    private List<String> deduplicate(List<String> values) {
        return values.stream()
                .filter(path -> path != null && !path.isBlank())
                .map(String::trim)
                .distinct()
                .collect(Collectors.toCollection(ArrayList::new));
    }
}