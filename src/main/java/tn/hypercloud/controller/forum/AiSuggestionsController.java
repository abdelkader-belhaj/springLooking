package tn.hypercloud.controller.forum;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import java.util.*;

@RestController
@RequestMapping("/api/ai")
@CrossOrigin(origins = "http://localhost:4200")
public class AiSuggestionsController {

    @Value("${groq.api.key}")
    private String groqApiKey;

    @Value("${tavily.api.key}")
    private String tavilyApiKey;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @PostMapping("/suggestions")
    public ResponseEntity<Map<String, Object>> getSuggestions(
            @RequestBody Map<String, String> body) {

        String title   = body.getOrDefault("title", "");
        String content = body.getOrDefault("content", "");

        try {
            // ── ÉTAPE 1 : Tavily cherche sur internet ──────────────────
            String searchQuery = title + " " + content;
            String webResults  = searchWeb(searchQuery);

            // ── ÉTAPE 2 : Groq génère la réponse avec les résultats ────
            String prompt =
                    "Un utilisateur a posté dans un forum :\n" +
                            "Titre: \"" + title + "\"\n" +
                            "Contenu: \"" + content + "\"\n\n" +
                            "Voici des informations trouvées sur internet :\n" +
                            webResults + "\n\n" +
                            "En te basant sur ces informations, génère une réponse utile " +
                            "et concise (max 3 phrases) pour enrichir la discussion.\n" +
                            "Réponds UNIQUEMENT en JSON valide sans markdown :\n" +
                            "{\"summary\": \"ta réponse ici\", " +
                            "\"sources\": [\"url1\", \"url2\"], " +
                            "\"suggestions\": [\"question1?\", \"question2?\", \"question3?\"]}";

            String aiResponse = callGroq(prompt);

            // Nettoyer backticks si présents
            aiResponse = aiResponse
                    .replaceAll("```json", "")
                    .replaceAll("```", "")
                    .trim();

            Map<String, Object> parsed =
                    objectMapper.readValue(aiResponse, Map.class);
            return ResponseEntity.ok(parsed);

        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Erreur AI: " + e.getMessage());
            return ResponseEntity.ok(Map.of(
                    "summary", "Informations non disponibles pour le moment.",
                    "sources", List.of(),
                    "suggestions", List.of(
                            "Qu'en pensez-vous ?",
                            "Avez-vous une expérience similaire ?",
                            "Quelles sont vos suggestions ?"
                    )
            ));
        }
    }

    // ── Tavily Web Search ───────────────────────────────────────────────
    private String searchWeb(String query) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + tavilyApiKey);

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("query", query);
            requestBody.put("max_results", 3);
            requestBody.put("search_depth", "basic");
            requestBody.put("include_answer", true);

            HttpEntity<Map<String, Object>> entity =
                    new HttpEntity<>(requestBody, headers);

            ResponseEntity<Map> response = restTemplate.postForEntity(
                    "https://api.tavily.com/search",
                    entity,
                    Map.class
            );

            // Extraire les résultats
            StringBuilder results = new StringBuilder();

            // Réponse directe Tavily si disponible
            Object answer = response.getBody().get("answer");
            if (answer != null) {
                results.append("Résumé: ").append(answer).append("\n\n");
            }

            // Les résultats individuels
            List<Map<String, Object>> resultsList =
                    (List<Map<String, Object>>) response.getBody().get("results");

            if (resultsList != null) {
                for (Map<String, Object> result : resultsList) {
                    results.append("Source: ").append(result.get("url")).append("\n");
                    results.append("Titre: ").append(result.get("title")).append("\n");
                    results.append("Contenu: ").append(result.get("content")).append("\n\n");
                }
            }

            return results.toString();

        } catch (Exception e) {
            System.err.println("Erreur Tavily: " + e.getMessage());
            return "Aucun résultat web trouvé.";
        }
    }

    // ── Groq AI Call ────────────────────────────────────────────────────
    private String callGroq(String prompt) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + groqApiKey);

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", "llama-3.3-70b-versatile");
        requestBody.put("max_tokens", 500);
        requestBody.put("temperature", 0.7);
        requestBody.put("messages", List.of(
                Map.of("role", "user", "content", prompt)
        ));

        HttpEntity<Map<String, Object>> entity =
                new HttpEntity<>(requestBody, headers);

        ResponseEntity<Map> response = restTemplate.postForEntity(
                "https://api.groq.com/openai/v1/chat/completions",
                entity,
                Map.class
        );

        List<Map<String, Object>> choices =
                (List<Map<String, Object>>) response.getBody().get("choices");
        Map<String, Object> messageObj =
                (Map<String, Object>) choices.get(0).get("message");

        return (String) messageObj.get("content");
    }
}