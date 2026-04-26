package tn.hypercloud.dto.event;

import jakarta.validation.constraints.NotBlank;
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
public class EventAiPriceSuggestionRequest {
    @NotBlank
    private String title;

    @NotBlank
    private String description;

    @NotBlank
    private String type;

    private String categoryName;
    private String city;
    private String address;
    private Integer capacity;
    private String startDate;
    private String endDate;
}