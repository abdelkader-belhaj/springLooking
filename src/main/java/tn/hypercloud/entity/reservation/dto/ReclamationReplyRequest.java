package tn.hypercloud.entity.reservation.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ReclamationReplyRequest {
    @NotBlank
    private String reponse;
}

