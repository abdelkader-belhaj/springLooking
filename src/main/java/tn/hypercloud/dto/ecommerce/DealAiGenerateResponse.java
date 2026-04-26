package tn.hypercloud.dto.ecommerce;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DealAiGenerateResponse {
    private String title;
    private String description;
    private String location;
    private String region;
    private String budget;
    private String activityType;
    private String environment;
    private String category;
    private String duration;
    private String imageUrl;   // relative path: /uploads/deals/filename.jpg
}
