package tn.hypercloud.dto.event;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EventAiAssistantRequest {

    @NotBlank(message = "message is required")
    private String message;

    private Integer maxResults;
}
