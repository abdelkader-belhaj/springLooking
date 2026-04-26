package tn.hypercloud.controller.accommodation;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

/**
 * Contrôleur IA dédié au logement.
 * Fournit des fonctionnalités d'amélioration de description via Groq (LLaMA 3.3).
 * NE PAS MERGER avec AiCorrectionController (forum).
 */
@RestController
@RequestMapping("/api/logement-ai")
@CrossOrigin(origins = "http://localhost:4200")
@RequiredArgsConstructor
public class LogementAiController {

    @Value("${groq.api.key}")
    private String groqApiKey;

    private final RestTemplate restTemplate;

    /**
     * Corrige l'orthographe et améliore le style d'une description de logement.
     * POST /api/logement-ai/enhance-description
     * Body  : { "text": "..." }
     * Return: { "corrected": "..." }
     */
    @PostMapping("/enhance-description")
    public ResponseEntity<Map<String, String>> enhanceDescription(
            @RequestBody Map<String, String> body) {

        String text = body.getOrDefault("text", "");
        if (text.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Le champ 'text' est requis."));
        }

        String systemPrompt = """
                Tu es un correcteur et rédacteur professionnel du français.
                Règles STRICTES :
                1. Corrige toutes les fautes d'orthographe et de grammaire.
                2. Tu peux améliorer légèrement le style et la fluidité de la phrase.
                3. INTERDIT de changer le contexte, le sens ou le sujet de la phrase.
                4. INTERDIT de rajouter des informations qui n'existent pas dans le texte original.
                5. Garde la même idée principale, le même ton et la même longueur approximative.
                6. Réponds UNIQUEMENT avec le texte corrigé, rien d'autre.
                """;

        String userPrompt = "Corrige et améliore légèrement la rédaction de ce texte sans changer son sens : \"%s\"".formatted(text);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + groqApiKey);

        Map<String, Object> requestBody = Map.of(
                "model",       "llama-3.3-70b-versatile",
                "temperature", 0.2,
                "max_tokens",  512,
                "messages", List.of(
                        Map.of("role", "system", "content", systemPrompt),
                        Map.of("role", "user",   "content", userPrompt)
                )
        );

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(
                    "https://api.groq.com/openai/v1/chat/completions",
                    new HttpEntity<>(requestBody, headers),
                    Map.class
            );

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> choices =
                    (List<Map<String, Object>>) response.getBody().get("choices");

            @SuppressWarnings("unchecked")
            String corrected = ((String) ((Map<String, Object>) choices.get(0).get("message")).get("content")).trim();

            // Retirer les guillemets encadrants si présents
            if (corrected.startsWith("\"") && corrected.endsWith("\"")) {
                corrected = corrected.substring(1, corrected.length() - 1);
            }

            return ResponseEntity.ok(Map.of("corrected", corrected));

        } catch (Exception e) {
            System.err.println("=== [LogementAI] Erreur Groq: " + e.getMessage());
            // Fallback : retourner le texte original sans erreur 500
            return ResponseEntity.ok(Map.of("corrected", text));
        }
    }
}