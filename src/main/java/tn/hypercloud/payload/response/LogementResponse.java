package tn.hypercloud.payload.response;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class LogementResponse {
    private Integer       idLogement;
    private Integer       idCategorie;
    private String        nomCategorie;
    private Long          idHebergeur;
    private String        nomHebergeur;
    private String        nom;
    private String        description;
    private String        imageUrl;          // Première image (rétro-compat)
    private List<String>  imageUrls;         // Toutes les images
    private String        videoUrl;
    private String        adresse;
    private String        ville;
    private BigDecimal    prixNuit;
    private int           capacite;
    private Integer       availablePlaces;
    private Boolean       saturated;
    private LocalDate     nextAvailableDate;
    private boolean       disponible;
    private Double        latitude;
    private Double        longitude;
    private LocalDateTime dateCreation;
}