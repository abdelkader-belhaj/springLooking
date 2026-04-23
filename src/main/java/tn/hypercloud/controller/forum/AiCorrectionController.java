package tn.hypercloud.controller.forum;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiCorrectionController {

    @Value("${groq.correction.api.key}")
    private String groqApiKey;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Map<String, Map<String, Object>> locationShares = new ConcurrentHashMap<>();

    @PostMapping("/correct")
    public ResponseEntity<Map<String, String>> correctText(
            @RequestBody Map<String, String> body) {

        String title   = body.getOrDefault("title", "");
        String content = body.getOrDefault("content", "");

        String systemPrompt = """
                Tu es un correcteur professionnel de texte français.
                Tu corriges TOUJOURS les fautes et reformules le texte.
                Tu réponds UNIQUEMENT en JSON valide, sans markdown, sans backticks.
                Format exact : {"correctedTitle":"...","correctedContent":"..."}
                """;

        String userPrompt = """
                Corrige et reformule professionnellement :
                - "Notr" doit devenir "Notre"
                - "pari" (ville) doit devenir "Paris"
                - Corrige toutes les fautes d'orthographe et de grammaire
                - Améliore le style pour qu'il soit professionnel

                Titre : "%s"
                Contenu : "%s"

                Réponds UNIQUEMENT avec le JSON, rien d'autre.
                """.formatted(title, content);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + groqApiKey);

        Map<String, Object> requestBody = Map.of(
                "model",       "llama-3.3-70b-versatile",
                "temperature", 0.1,
                "max_tokens",  512,
                "messages", List.of(
                        Map.of("role", "system",  "content", systemPrompt),
                        Map.of("role", "user",    "content", userPrompt)
                )
        );

        HttpEntity<Map<String, Object>> entity =
                new HttpEntity<>(requestBody, headers);

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(
                    "https://api.groq.com/openai/v1/chat/completions",
                    entity,
                    Map.class
            );

            // Parser réponse Groq (format OpenAI)
            List<Map<String, Object>> choices =
                    (List<Map<String, Object>>) response.getBody().get("choices");
            Map<String, Object> message =
                    (Map<String, Object>) choices.get(0).get("message");
            String rawText = ((String) message.get("content")).trim();

            System.out.println("=== Groq response: " + rawText); // debug

            // Nettoyage robuste
            rawText = rawText.replaceAll("(?s)```json\\s*", "")
                    .replaceAll("(?s)```\\s*", "")
                    .trim();

            // Extraire uniquement le JSON
            int start = rawText.indexOf('{');
            int end   = rawText.lastIndexOf('}');
            if (start != -1 && end != -1) {
                rawText = rawText.substring(start, end + 1);
            }

            Map<String, String> result = objectMapper.readValue(rawText, Map.class);
            return ResponseEntity.ok(result);

        } catch (Exception e) {
            System.err.println("=== Erreur Groq: " + e.getMessage());
            return ResponseEntity.ok(Map.of(
                    "correctedTitle",   title,
                    "correctedContent", content
            ));
        }
    }
    @PostMapping("/location-message")
    public ResponseEntity<Map<String, Object>> generateLocationMessage(
            @RequestBody Map<String, String> body) {

        String username = body.getOrDefault("username", "Utilisateur");
        String community = body.getOrDefault("community", "la communauté");

        String prompt = """
        Génère un message de partage de position pour un forum communautaire.
        Utilisateur : %s
        Communauté : %s

        Réponds UNIQUEMENT en JSON sans backticks :
        {
          "message": "message court et amical annonçant le partage de position",
          "options": [
            {"label": "15 minutes", "minutes": 15, "description": "une description courte"},
            {"label": "1 heure",    "minutes": 60, "description": "une description courte"},
            {"label": "2 heures",   "minutes": 120,"description": "une description courte"}
          ]
        }
        """.formatted(username, community);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + groqApiKey);

        Map<String, Object> requestBody = Map.of(
                "model", "llama-3.3-70b-versatile",
                "temperature", 0.7,
                "max_tokens", 400,
                "messages", List.of(
                        Map.of("role", "system",  "content", "Tu es un assistant pour application communautaire. Réponds uniquement en JSON valide."),
                        Map.of("role", "user",    "content", prompt)
                )
        );

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(
                    "https://api.groq.com/openai/v1/chat/completions",
                    new HttpEntity<>(requestBody, headers),
                    Map.class
            );
            List<Map<String,Object>> choices = (List) response.getBody().get("choices");
            String raw = ((String)((Map)choices.get(0).get("message")).get("content")).trim();

            raw = raw.replaceAll("(?s)```json\\s*","").replaceAll("(?s)```\\s*","").trim();
            int s = raw.indexOf('{'), e = raw.lastIndexOf('}');
            if (s != -1 && e != -1) raw = raw.substring(s, e + 1);

            Map<String, Object> result = new ObjectMapper().readValue(raw, Map.class);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of(
                    "message", username + " partage sa position avec " + community,
                    "options", List.of(
                            Map.of("label","15 minutes","minutes",15,"description","Partage rapide"),
                            Map.of("label","1 heure",   "minutes",60,"description","Recommandé"),
                            Map.of("label","2 heures",  "minutes",120,"description","Partage prolongé")
                    )
            ));
        }
    }
    @PostMapping("/location-share")
    public ResponseEntity<?> saveLocationShare(@RequestBody Map<String, Object> body) {
        String key = body.get("userId") + "_" + body.get("communityId");
        body.put("expiresAt", Instant.now().plusSeconds(
                ((Number) body.getOrDefault("minutes", 60)).longValue() * 60
        ).toString());
        body.put("startedAt", Instant.now().toString());
        locationShares.put(key, body);
        return ResponseEntity.ok(body);
    }

    @DeleteMapping("/location-share/{userId}")
    public ResponseEntity<?> deleteLocationShare(
            @PathVariable String userId,
            @RequestParam String communityId) {
        locationShares.remove(userId + "_" + communityId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/location-share/community/{communityId}")
    public ResponseEntity<List<Map<String, Object>>> getLocationShares(
            @PathVariable String communityId) {
        Instant now = Instant.now();
        List<Map<String, Object>> result = locationShares.values().stream()
                .filter(s -> communityId.equals(String.valueOf(s.get("communityId"))))
                .filter(s -> Instant.parse((String) s.get("expiresAt")).isAfter(now))
                .collect(Collectors.toList());
        return ResponseEntity.ok(result);
    }
}