package tn.hypercloud.service.event;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.HttpStatusCodeException;
import tn.hypercloud.entity.event.EventReservation;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Locale;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class EventVisionAiService {

    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate;

    @Value("${gemini.api.key:}")
    private String geminiApiKey;

    @Value("${gemini.api.url:}")
    private String geminiApiUrl;

    @Value("${gemini.api.base-url:https://generativelanguage.googleapis.com/v1beta/models}")
    private String geminiApiBaseUrl;

    @Value("${gemini.model:gemini-2.0-flash}")
    private String geminiModel;

    @Value("${vision.ai.provider:groq}")
    private String visionProvider;

    @Value("${vision.ai.api.key:}")
    private String visionApiKey;

    @Value("${vision.ai.api.url:https://api.groq.com/openai/v1/chat/completions}")
    private String visionApiUrl;

    @Value("${vision.ai.model:llama-3.2-11b-vision-preview}")
    private String visionModel;

    public VisionAnalysisResult analyzeTicketImage(String imageBase64, EventReservation reservation) {
        String cleanBase64 = imageBase64;
        String mimeType = "image/jpeg";

        if (imageBase64 != null && imageBase64.contains(",")) {
            String[] parts = imageBase64.split(",", 2);
            cleanBase64 = parts[1];
            if (parts[0].contains("png")) {
                mimeType = "image/png";
            } else if (parts[0].contains("webp")) {
                mimeType = "image/webp";
            }
        }

        String prompt = """
                Tu es un validateur de billets d'événements en Tunisie.

                Cette image contient un billet électronique avec un QR code.

                TÂCHES :
                1. Lis et décode le QR code visible sur l'image
                2. Extrais l'ID de réservation du QR code ou du texte visible sur le billet
                3. Vérifie la cohérence avec la réservation attendue :
                   - ID attendu : %d
                   - Client attendu : %s
                   - Événement attendu : %s
                   - Statut actuel en base : %s

                RÈGLES DE VALIDATION :
                - valid = true UNIQUEMENT si :
                  * Le QR code est lisible et authentique
                  * L'ID détecté correspond à l'ID attendu (%d)
                  * Le statut est CONFIRMED
                - valid = false si :
                  * QR illisible ou falsifié
                  * ID ne correspond pas
                  * Ticket déjà utilisé (USED) ou annulé (CANCELLED)
                - reservationIdDetected = l'ID que tu lis dans le QR ou sur le billet (null si illisible)
                - extractedData = résumé de tout ce que tu vois sur le billet
                - message = 1 phrase courte en français expliquant le résultat

                Réponds UNIQUEMENT avec ce JSON exact, sans markdown, sans texte avant ou après :
                {
                  "valid": true ou false,
                  "message": "message court en français",
                  "extractedData": "ce que tu vois sur le billet",
                  "reservationIdDetected": null ou nombre entier
                }
                """.formatted(
                reservation.getId(),
                reservation.getUser() != null
                        ? reservation.getUser().getFullName()

                        : "Inconnu",
                reservation.getEvent() != null
                        ? reservation.getEvent().getTitle()
                        : "Inconnu",
                reservation.getStatus() != null
                        ? reservation.getStatus().name()
                        : "INCONNU",
                reservation.getId()
        );

        String provider = (visionProvider == null ? "" : visionProvider.trim()).toLowerCase(Locale.ROOT);
        if (provider.isBlank() || provider.equals("groq") || provider.equals("qwen") || provider.equals("openai")) {
            VisionAnalysisResult result = analyzeWithOpenAiCompatible(prompt, cleanBase64, mimeType);
            if (result != null) {
                return result;
            }
        }

        return analyzeWithGemini(prompt, cleanBase64, mimeType);
    }

    private VisionAnalysisResult analyzeWithOpenAiCompatible(String prompt, String cleanBase64, String mimeType) {
        String apiKey = !isBlank(visionApiKey) ? visionApiKey.trim() : (geminiApiKey == null ? "" : geminiApiKey.trim());
        String apiUrl = !isBlank(visionApiUrl) ? visionApiUrl.trim() : "https://api.groq.com/openai/v1/chat/completions";
        String model = !isBlank(visionModel) ? visionModel.trim() : "llama-3.2-11b-vision-preview";

        if (apiKey.isBlank()) {
            return null;
        }

        String imageDataUrl = "data:%s;base64,%s".formatted(mimeType, cleanBase64);

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);

            Map<String, Object> payload = Map.of(
                    "model", model,
                    "temperature", 0.0,
                    "max_tokens", 500,
                    "messages", List.of(
                            Map.of("role", "system", "content",
                                    "Tu es un validateur de billets QR. Réponds uniquement en JSON strict sans markdown."),
                            Map.of("role", "user", "content", List.of(
                                    Map.of("type", "text", "text", prompt),
                                    Map.of("type", "image_url", "image_url", Map.of("url", imageDataUrl))
                            ))
                    )
            );

            ResponseEntity<String> response = restTemplate.postForEntity(
                    apiUrl,
                    new HttpEntity<>(payload, headers),
                    String.class
            );

            return parseOpenAiCompatibleResponse(response.getBody());
        } catch (Exception ignored) {
            return null;
        }
    }

    private VisionAnalysisResult analyzeWithGemini(String prompt, String cleanBase64, String mimeType) {
        if (geminiApiKey == null || geminiApiKey.isBlank()) {
            return VisionAnalysisResult.invalid(
                    "Service IA Vision indisponible : clé Qwen/Groq ou Gemini non configurée.");
        }

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> payload = Map.of(
                    "contents", List.of(
                            Map.of("parts", List.of(
                                    Map.of("text", prompt),
                                    Map.of("inline_data", Map.of(
                                            "mime_type", mimeType,
                                            "data", cleanBase64
                                    ))
                            ))
                    ),
                    "generationConfig", Map.of(
                            "temperature", 0.0,
                            "maxOutputTokens", 400
                    )
            );

            String lastError = "Service Gemini indisponible.";
            for (String endpoint : resolveCandidateEndpoints()) {
                try {
                    ResponseEntity<String> response = restTemplate.postForEntity(
                            endpoint + "?key=" + geminiApiKey.trim(),
                            new HttpEntity<>(payload, headers),
                            String.class
                    );
                    return parseJsonResultText(extractGeminiText(response.getBody()));
                } catch (HttpStatusCodeException ex) {
                    if (ex.getStatusCode().value() == 404) {
                        lastError = "Modele Gemini indisponible sur cet endpoint, tentative suivante...";
                        continue;
                    }
                    lastError = "Échec appel IA Vision : " + ex.getStatusCode().value() + " " + ex.getStatusText();
                    break;
                }
            }

            return VisionAnalysisResult.invalid(lastError);

        } catch (Exception ex) {
            return VisionAnalysisResult.invalid("Échec appel IA Vision : " + ex.getMessage());
        }
    }

    private List<String> resolveCandidateEndpoints() {
        String explicitUrl = geminiApiUrl == null ? "" : geminiApiUrl.trim();
        if (!explicitUrl.isBlank()) {
            URI uri = URI.create(explicitUrl);
            if (!uri.isAbsolute()) {
                return List.of();
            }
            return List.of(explicitUrl);
        }

        String computedBase = (geminiApiBaseUrl == null ? "" : geminiApiBaseUrl.trim()).replaceAll("/+$", "");
        if (computedBase.isBlank()) {
            computedBase = "https://generativelanguage.googleapis.com/v1beta/models";
        }
        final String base = computedBase;

        List<String> models = new ArrayList<>();
        if (geminiModel != null && !geminiModel.isBlank()) {
            models.add(geminiModel.trim());
        }
        models.addAll(Arrays.asList(
            "gemini-2.0-flash",
            "gemini-2.0-flash-lite",
            "gemini-1.5-flash-latest",
            "gemini-1.5-flash-002",
            "gemini-1.5-pro-latest"
        ));

        return models.stream()
                .map(model -> base + "/" + model + ":generateContent")
                .distinct()
                .toList();
    }

    private String extractGeminiText(String payload) {
        try {
            JsonNode root = objectMapper.readTree(payload);

            return root
                    .path("candidates")
                    .path(0)
                    .path("content")
                    .path("parts")
                    .path(0)
                    .path("text")
                    .asText("")
                    .trim();
        } catch (Exception ex) {
            return "";
        }
    }

    private VisionAnalysisResult parseOpenAiCompatibleResponse(String payload) {
        try {
            JsonNode root = objectMapper.readTree(payload);
            JsonNode contentNode = root.path("choices").path(0).path("message").path("content");

            String content;
            if (contentNode.isTextual()) {
                content = contentNode.asText("").trim();
            } else if (contentNode.isArray() && contentNode.size() > 0) {
                content = contentNode.path(0).path("text").asText("").trim();
            } else {
                content = "";
            }

            return parseJsonResultText(content);
        } catch (Exception ex) {
            return VisionAnalysisResult.invalid("Réponse IA non exploitable : " + ex.getMessage());
        }
    }

    private VisionAnalysisResult parseJsonResultText(String content) {
        try {
            if (content.isBlank()) {
                return VisionAnalysisResult.invalid("Réponse IA vide.");
            }

            if (content.isBlank()) {
                return VisionAnalysisResult.invalid("Réponse IA vide.");
            }

            String cleaned = content
                    .replaceAll("(?s)```json\\s*", "")
                    .replaceAll("(?s)```\\s*", "")
                    .trim();

            JsonNode json = objectMapper.readTree(cleaned);

            boolean valid = json.path("valid").asBoolean(false);
            String message = json.path("message").asText("Analyse IA terminée.");
            String extractedData = json.path("extractedData").asText(null);
            Integer detectedId = json.path("reservationIdDetected").isNumber()
                    ? json.path("reservationIdDetected").asInt()
                    : null;

            return new VisionAnalysisResult(valid, message, extractedData, detectedId);

        } catch (Exception ex) {
            return VisionAnalysisResult.invalid("Réponse IA non exploitable : " + ex.getMessage());
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    @Getter
    @AllArgsConstructor
    public static class VisionAnalysisResult {
        private final boolean valid;
        private final String message;
        private final String extractedData;
        private final Integer reservationIdDetected;

        public static VisionAnalysisResult invalid(String message) {
            return new VisionAnalysisResult(false, message, null, null);
        }
    }
}