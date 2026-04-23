package tn.hypercloud.entity.reservation.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class HotelRecommendation {
    private String nom;
    private String prixApprox;
    private double latitude;
    private double longitude;
    private String osmLink;
}
