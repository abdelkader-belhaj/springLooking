package tn.hypercloud.entity.accommodation;

import jakarta.persistence.*;
import lombok.*;
import tn.hypercloud.entity.user.User;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import com.fasterxml.jackson.databind.ObjectMapper;

@Entity
@Table(name = "logement")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Logement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_logement")
    private Integer idLogement;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_categorie", nullable = false)
    private Categorie categorie;

    // ✅ AJOUT : lien avec l'hébergeur
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_hebergeur", nullable = false)
    private User hebergeur;

    @Column(nullable = false, length = 150)
    private String nom;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    @Column(name = "image_urls", columnDefinition = "LONGTEXT")
    private String imageUrlsJson;  // Stockage JSON des images multiples

    @Column(name = "video_url", length = 500)
    private String videoUrl;

    @Column(length = 255)
    private String adresse;

    @Column(length = 100)
    private String ville;

    @Column(name = "prix_nuit", nullable = false, precision = 10, scale = 2)
    private BigDecimal prixNuit;

    @Column(nullable = false)
    private int capacite = 1;

    @Column(nullable = false)
    private boolean disponible = true;

    // ✅ AJOUT : Champs de Géolocalisation pour "Geo-Secured Access"
    @Column(name = "latitude")
    private Double latitude;

    @Column(name = "longitude")
    private Double longitude;

    @Column(name = "date_creation", updatable = false)
    private LocalDateTime dateCreation;

    @PrePersist
    protected void onCreate() {
        dateCreation = LocalDateTime.now();
    }

    // Méthodes utilitaires pour gérer les images multiples
    public List<String> getImageUrls() {
        if (imageUrlsJson == null || imageUrlsJson.isEmpty()) {
            return new ArrayList<>();
        }
        try {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.readValue(imageUrlsJson,
                    mapper.getTypeFactory().constructCollectionType(List.class, String.class));
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    public void setImageUrls(List<String> urls) {
        if (urls == null || urls.isEmpty()) {
            this.imageUrlsJson = null;
        } else {
            try {
                ObjectMapper mapper = new ObjectMapper();
                this.imageUrlsJson = mapper.writeValueAsString(urls);
            } catch (Exception e) {
                this.imageUrlsJson = null;
            }
        }
    }
}