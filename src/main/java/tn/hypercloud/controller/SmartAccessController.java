package tn.hypercloud.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.hypercloud.payload.request.VerifyLocationRequest;
import tn.hypercloud.payload.response.GeoAccessResponse;
import tn.hypercloud.service.accommodation.SmartAccessService;

@RestController
@RequestMapping("/api/smart-access")
@RequiredArgsConstructor
@CrossOrigin(origins = "*") // Activer CORS pour les tests locaux Angular
public class SmartAccessController {

    private final SmartAccessService smartAccessService;

    @PostMapping("/verify-location")
    public ResponseEntity<GeoAccessResponse> verifyLocation(@RequestBody VerifyLocationRequest request) {
        return ResponseEntity.ok(smartAccessService.verifyLocationAndUnlock(request));
    }
}
