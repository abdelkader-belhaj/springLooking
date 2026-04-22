package tn.hypercloud.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Service
public class GoogleTokenVerifierService {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${app.google.client-id:}")
    private String googleClientId;

    @Value("${app.google.token-info-url:https://oauth2.googleapis.com/tokeninfo?id_token=}")
    private String tokenInfoUrl;

    public GoogleUserInfo verifyIdToken(String idToken) {
        if (idToken == null || idToken.isBlank()) {
            throw new RuntimeException("Token Google invalide");
        }

        if (googleClientId == null || googleClientId.isBlank()) {
            throw new RuntimeException("Configuration Google manquante (app.google.client-id)");
        }

        ResponseEntity<Map> response;
        try {
            response = restTemplate.getForEntity(tokenInfoUrl + idToken, Map.class);
        } catch (Exception ex) {
            throw new RuntimeException("Token Google invalide ou expire");
        }

        Map<?, ?> body = response.getBody();
        if (body == null) {
            throw new RuntimeException("Reponse Google invalide");
        }

        String audience = readString(body, "aud");
        if (!googleClientId.equals(audience)) {
            throw new RuntimeException("Audience Google invalide");
        }

        String issuer = readString(body, "iss");
        boolean issuerValid = "accounts.google.com".equals(issuer)
                || "https://accounts.google.com".equals(issuer);
        if (!issuerValid) {
            throw new RuntimeException("Issuer Google invalide");
        }

        String email = readString(body, "email");
        String emailVerified = readString(body, "email_verified");
        if (email == null || email.isBlank() || !"true".equalsIgnoreCase(emailVerified)) {
            throw new RuntimeException("Email Google non verifie");
        }

        String subject = readString(body, "sub");
        String name = readString(body, "name");
        String picture = readString(body, "picture");

        return new GoogleUserInfo(subject, email, name, picture);
    }

    private String readString(Map<?, ?> source, String key) {
        Object value = source.get(key);
        return value != null ? String.valueOf(value) : null;
    }

    public record GoogleUserInfo(String sub, String email, String name, String picture) {
    }
}