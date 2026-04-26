package tn.hypercloud.payload.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecommendationResponse {
    private LogementResponse logement;
    private double aiScore; // Score IA (les "étoiles" sur 5)
}
