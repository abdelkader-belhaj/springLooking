package tn.hypercloud.entity.reservation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ReclamationUpdateRequest {

    @NotNull
    private ReclamationCreateRequest.Priorite priorite;

    @NotBlank
    private String sujet;
}

