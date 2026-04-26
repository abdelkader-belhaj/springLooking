package tn.hypercloud.controller.ecommerce;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.hypercloud.dto.ecommerce.DealAiGenerateRequest;
import tn.hypercloud.dto.ecommerce.DealAiGenerateResponse;
import tn.hypercloud.service.ecommerce.DealAiService;

@RestController
@RequestMapping("/api/deals")
@RequiredArgsConstructor
public class DealAiController {

    private final DealAiService dealAiService;

    @PostMapping("/ai-generate")
    public ResponseEntity<DealAiGenerateResponse> generate(
        @RequestBody DealAiGenerateRequest request
    ) {
        return ResponseEntity.ok(dealAiService.generate(request.description()));
    }
}
