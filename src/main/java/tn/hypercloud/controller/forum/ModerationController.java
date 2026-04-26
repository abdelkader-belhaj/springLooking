package tn.hypercloud.controller.forum;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@RestController
@RequestMapping("/api/moderation")
public class ModerationController {

    @Value("${huggingface.api.token}")
    private String hfToken;

    private final RestTemplate restTemplate;

    public ModerationController(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    private final List<String> forbiddenWords = List.of(
            "raciste", "haine", "idiot", "spam", "mort",
            "tuer", "violence", "nazi", "terroriste", "insulte"
    );

    private static final String HF_URL =
            "https://api-inference.huggingface.co/models/martin-ha/toxic-comment-model";

    @PostMapping("/analyze")
    public ResponseEntity<Map<String, Object>> analyze(
            @RequestBody Map<String, String> body) {

        String content = body.getOrDefault("content", "");

        // 1. Pré-filtre local
        String foundWord = forbiddenWords.stream()
                .filter(w -> content.toLowerCase().contains(w))
                .findFirst().orElse(null);

        if (foundWord != null) {
            return ResponseEntity.ok(Map.of(
                    "approved", false,
                    "score", 0.99,
                    "category", "OFFENSIVE",
                    "reason", "Mot interdit : " + foundWord
            ));
        }

        // 2. Analyse IA Hugging Face
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + hfToken);
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, String> requestBody = Map.of("inputs", content);
            HttpEntity<Map<String, String>> request = new HttpEntity<>(requestBody, headers);

            ResponseEntity<Object> response = restTemplate.postForEntity(
                    HF_URL, request, Object.class
            );

            double toxicScore = extractToxicScore(response.getBody());
            boolean approved = toxicScore < 0.7;

            Map<String, Object> result = new HashMap<>();
            result.put("approved", approved);
            result.put("score", toxicScore);
            result.put("category", approved ? "SAFE" : "TOXIC");
            if (!approved) {
                result.put("reason", "Contenu toxique détecté par l'IA");
            }
            return ResponseEntity.ok(result);

        } catch (Exception e) {
            System.err.println("Hugging Face erreur: " + e.getMessage());
            // Si HF indisponible → approuver par défaut
            return ResponseEntity.ok(Map.of(
                    "approved", true,
                    "score", 0.0,
                    "category", "SAFE"
            ));
        }
    }

    @SuppressWarnings("unchecked")
    private double extractToxicScore(Object responseBody) {
        try {
            List<List<Map<String, Object>>> results =
                    (List<List<Map<String, Object>>>) responseBody;
            return results.get(0).stream()
                    .filter(r -> "toxic".equalsIgnoreCase((String) r.get("label")))
                    .mapToDouble(r -> ((Number) r.get("score")).doubleValue())
                    .findFirst()
                    .orElse(0.0);
        } catch (Exception e) {
            return 0.0;
        }
    }
}