package tn.hypercloud.dto.ecommerce;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class DealDTO {
    private Long id;
    private String title;
    private String description;
    private String location;
    private String region;
    private String budget;
    private String image;
    private String activityType;
    private String environment;
    private String category;
    private String duration;
    private int favoritesCount;
}
