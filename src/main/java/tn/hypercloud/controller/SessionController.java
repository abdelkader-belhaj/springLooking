package tn.hypercloud.controller;

import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tn.hypercloud.entity.user.User;
import tn.hypercloud.payload.response.ApiResponse;
import tn.hypercloud.payload.response.SessionInfoResponse;

import java.util.Map;

@RestController
@RequestMapping("/api/session")
public class SessionController {

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<SessionInfoResponse>> mySession(
            Authentication authentication,
            HttpSession session) {

        User user = extractUser(authentication);
        SessionInfoResponse info = SessionInfoResponse.from(session, user);
        return ResponseEntity.ok(ApiResponse.success("Session utilisateur", info));
    }

    @GetMapping("/my-section")
    public ResponseEntity<ApiResponse<Map<String, String>>> myRoleSection(Authentication authentication) {
        String role = extractRole(authentication);
        String section = switch (role) {
            case "ADMIN" -> "section-admin";
            case "CLIENT_TOURISTE" -> "section-client-touriste";
            case "HEBERGEUR" -> "section-hebergeur";
            case "TRANSPORTEUR" -> "section-transporteur";
            case "AIRLINE_PARTNER" -> "section-airline-partner";
            case "ORGANISATEUR" -> "section-organisateur";
            case "VENDEUR_ARTI" -> "section-vendeur-arti";
            case "SOCIETE" -> "section-societe";
            default -> "section-generale";
        };

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Section selon le role",
                        Map.of("role", role, "section", section)
                )
        );
    }

    private User extractUser(Authentication authentication) {
        Object principal = authentication.getPrincipal();
        if (principal instanceof User user) {
            return user;
        }
        throw new RuntimeException("Utilisateur non authentifie");
    }

    private String extractRole(Authentication authentication) {
        return authentication.getAuthorities()
                .stream()
                .map(GrantedAuthority::getAuthority)
                .filter(auth -> auth.startsWith("ROLE_"))
                .map(auth -> auth.substring(5))
                .findFirst()
                .orElse("UNKNOWN");
    }
}