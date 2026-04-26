package tn.hypercloud.payload.request;

import lombok.*;
import java.math.BigDecimal;
import java.util.List;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class LogementRequest {
    private Integer       idCategorie;
    private String        nom;
    private String        description;
    private String        imageUrl;          // Pour rétro-compatibilité
    private List<String>  imageUrls;         // Nouvelles images multiples
    private String        videoUrl;
    private String        adresse;
    private String        ville;
    private BigDecimal    prixNuit;
    private int           capacite;
    private boolean       disponible;
    private Double        latitude;
    private Double        longitude;
}