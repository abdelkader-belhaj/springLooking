package tn.hypercloud.payload.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserPreferencesResponse {
    private Long id;
    private Integer userId;
    private Double budgetMax;
    private String typeSejour;
    private String villePreferee;
    private Integer capaciteMin;
    private String equipementsSouhaites;
    private String ambiance;
}
