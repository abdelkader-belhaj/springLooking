package tn.hypercloud.dto.event;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class EventActivityRequest {

    @NotBlank(message = "Le titre est obligatoire")
    @Size(max = 200)
    private String title;

    @NotBlank(message = "La description est obligatoire")
    private String description;

    @NotNull(message = "Le prix est obligatoire")
    @Positive(message = "Le prix doit être positif")
    private BigDecimal price;

    @Positive(message = "La capacité doit être positive")
    private int capacity;

    @NotNull(message = "La date de début est obligatoire")
    private LocalDateTime startDate;

    @NotNull(message = "La date de fin est obligatoire")
    private LocalDateTime endDate;

    @NotBlank(message = "La ville est obligatoire")
    @Size(max = 100)
    private String city;

    @NotBlank(message = "L'adresse est obligatoire")
    @Size(max = 255)
    private String address;

    private Double latitude;
    private Double longitude;

    private String imageUrl;

    @NotBlank(message = "Le type (EVENT ou ACTIVITY) est obligatoire")
    private String type;

    @NotNull(message = "La catégorie est obligatoire")
    private Integer categoryId;
}