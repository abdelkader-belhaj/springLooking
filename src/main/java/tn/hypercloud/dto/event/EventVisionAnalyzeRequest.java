package tn.hypercloud.dto.event;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EventVisionAnalyzeRequest {

    @NotBlank(message = "imageBase64 is required")
    private String imageBase64;

    @NotNull(message = "reservationId is required")
    private Integer reservationId;
}
