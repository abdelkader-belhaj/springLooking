package tn.hypercloud.controller.event;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tn.hypercloud.dto.event.EventAiAssistantRequest;
import tn.hypercloud.dto.event.EventAiAssistantResponse;
import tn.hypercloud.dto.event.EventAiPriceSuggestionRequest;
import tn.hypercloud.dto.event.EventAiPriceSuggestionResponse;
import tn.hypercloud.service.event.EventAiAssistantService;

@RestController
@RequestMapping("/api/events/assistant")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:4200")
public class EventAiAssistantController {

    private final EventAiAssistantService assistantService;

    @PostMapping("/recommendations")
    @PreAuthorize("permitAll()")
    public ResponseEntity<EventAiAssistantResponse> getRecommendations(
            @Valid @RequestBody EventAiAssistantRequest request
    ) {
        EventAiAssistantResponse response = assistantService.recommend(
                request.getMessage(),
                request.getMaxResults()
        );
        return ResponseEntity.ok(response);
    }

    @PostMapping("/price-suggestion")
    @PreAuthorize("permitAll()")
    public ResponseEntity<EventAiPriceSuggestionResponse> suggestPrice(
            @Valid @RequestBody EventAiPriceSuggestionRequest request
    ) {
        EventAiPriceSuggestionResponse response = assistantService.suggestPrice(request);
        return ResponseEntity.ok(response);
    }
}
