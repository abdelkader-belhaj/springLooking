package tn.hypercloud.payload.request;

import lombok.Data;

@Data
public class UserPreferencesRequest {
    private Double budgetMax;
    private String typeSejour;
    private String villePreferee;
    private Integer capaciteMin;
    private String equipementsSouhaites;
    private String ambiance;
}
