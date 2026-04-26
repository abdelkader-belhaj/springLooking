package tn.hypercloud.entity.reservation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ReclamationCreateRequest {

    @NotNull
    private Integer reservationId;

    @NotNull
    private Priorite priorite;

    @NotBlank
    private String sujet;

    public enum Priorite {
        urgent,
        normale,
        tres_urgent
    }
}

