package tn.hypercloud.dto.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventAiPriceSuggestionResponse {
    private Integer price;
    private String label;
    private String rationale;
    private boolean aiUsed;
}