package tn.hypercloud.entity.reservation.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class OffreResponse {
    private Integer id;
    private String code;
    private double pourcentage;
}
