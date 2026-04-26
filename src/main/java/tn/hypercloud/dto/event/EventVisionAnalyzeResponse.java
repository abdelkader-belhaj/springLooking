package tn.hypercloud.dto.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EventVisionAnalyzeResponse {
    private boolean valid;
    private String message;
    private String extractedData;
    private Integer reservationId;
    private Boolean alreadyUsed;
}
