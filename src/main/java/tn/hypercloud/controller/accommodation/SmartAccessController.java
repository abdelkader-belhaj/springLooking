package tn.hypercloud.controller.accommodation;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import tn.hypercloud.payload.request.VerifyLocationRequest;
import tn.hypercloud.payload.response.GeoAccessResponse;
import tn.hypercloud.service.accommodation.SmartAccessService;

@RestController
@RequestMapping("/api/smart-access")
@RequiredArgsConstructor
public class SmartAccessController {

    private final SmartAccessService smartAccessService;

    @PostMapping("/verify-location")
    public ResponseEntity<GeoAccessResponse> verifyLocation(@RequestBody VerifyLocationRequest request) {
        GeoAccessResponse response = smartAccessService.verifyLocationAndUnlock(request);
        return ResponseEntity.ok(response);
    }
}
