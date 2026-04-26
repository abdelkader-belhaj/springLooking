package tn.hypercloud.controller.accommodation;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import tn.hypercloud.entity.user.User;
import tn.hypercloud.payload.request.UserPreferencesRequest;
import tn.hypercloud.payload.response.UserPreferencesResponse;
import tn.hypercloud.repository.user.UserRepository;
import tn.hypercloud.service.accommodation.UserPreferencesService;

@RestController
@RequestMapping("/api/preferences")
@RequiredArgsConstructor
public class UserPreferencesController {

    private final UserPreferencesService prefsService;
    private final UserRepository userRepository;

    @PostMapping
    public ResponseEntity<UserPreferencesResponse> savePreferences(
            @RequestBody UserPreferencesRequest req,
            Authentication authentication) {
        User user = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("Utilisateur introuvable"));
        return ResponseEntity.ok(prefsService.saveOrUpdate(user.getId().intValue(), req));
    }

    @GetMapping
    public ResponseEntity<UserPreferencesResponse> getPreferences(Authentication authentication) {
        User user = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("Utilisateur introuvable"));
        return prefsService.getByUserId(user.getId().intValue())
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.noContent().build());
    }
}
