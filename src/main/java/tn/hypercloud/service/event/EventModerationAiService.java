package tn.hypercloud.service.event;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import tn.hypercloud.dto.event.EventActivityRequest;

import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class EventModerationAiService {

    @Value("${moderation.ai.api.url:${groq.api.url:https://api.groq.com/openai/v1/chat/completions}}")
    private String moderationApiUrl;

    @Value("${moderation.ai.api.key:${groq.api.key:}}")
    private String moderationApiKey;

    @Value("${moderation.ai.model:${groq.model:qwen3-32b}}")
    private String moderationModel;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ModerationDecision moderate(EventActivityRequest request) {
        if (moderationApiKey == null || moderationApiKey.isBlank()) {
            return null;
        }

        try {
            String userPrompt = buildUserPrompt(request);
            String systemPrompt = "Tu es un moderateur strict d'evenements. Reponds uniquement en JSON valide avec EXACTEMENT ces cles: approved(boolean), score(number 0-100), reason(string).";
                String selectedModel = resolveModelName(moderationModel);

            Map<String, Object> body = Map.of(
                    "model", selectedModel,
                    "temperature", 0.1,
                    "max_tokens", 220,
                    "messages", List.of(
                            Map.of("role", "system", "content", systemPrompt),
                            Map.of("role", "user", "content", userPrompt)
                    )
            );

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(moderationApiKey);

            ResponseEntity<String> response = restTemplate.postForEntity(
                    moderationApiUrl,
                    new HttpEntity<>(body, headers),
                    String.class
            );

            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                log.warn("Moderation API call failed: status={}", response.getStatusCode());
                return null;
            }

            return parseModerationResponse(response.getBody());
        } catch (Exception ex) {
            log.warn("Moderation API error: {}", ex.getMessage());
            return null;
        }
    }

    private String resolveModelName(String rawModel) {
        String model = rawModel == null ? "" : rawModel.trim();
        if (model.isEmpty()) {
            return "qwen/qwen3-32b";
        }
        if ("qwen3-32b".equalsIgnoreCase(model)) {
            return "qwen/qwen3-32b";
        }
        return model;
    }

    private ModerationDecision parseModerationResponse(String responseBody) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode contentNode = root.path("choices").path(0).path("message").path("content");
            if (contentNode.isMissingNode() || contentNode.asText().isBlank()) {
                return null;
            }

            String content = contentNode.asText().trim();
            JsonNode moderationJson;

            if (content.startsWith("{") && content.endsWith("}")) {
                moderationJson = objectMapper.readTree(content);
            } else {
                int first = content.indexOf('{');
                int last = content.lastIndexOf('}');
                if (first < 0 || last <= first) {
                    return null;
                }
                moderationJson = objectMapper.readTree(content.substring(first, last + 1));
            }

            boolean approved = readApproved(moderationJson.path("approved"));
            int score = normalizeScore(moderationJson.path("score"));
            String reason = moderationJson.path("reason").asText("Decision de moderation IA").trim();
            if (reason.isEmpty()) {
                reason = "Decision de moderation IA";
            }

            return new ModerationDecision(approved, score, reason);
        } catch (Exception ex) {
            log.warn("Failed to parse moderation payload: {}", ex.getMessage());
            return null;
        }
    }

    private boolean readApproved(JsonNode approvedNode) {
        if (approvedNode.isBoolean()) {
            return approvedNode.asBoolean();
        }
        String value = approvedNode.asText("").trim().toLowerCase();
        return "true".equals(value)
                || "approved".equals(value)
                || "accept".equals(value)
                || "accepted".equals(value)
                || "allow".equals(value);
    }

    private int normalizeScore(JsonNode scoreNode) {
        int score;
        if (scoreNode.isNumber()) {
            score = scoreNode.asInt();
        } else {
            String raw = scoreNode.asText("70").replaceAll("[^0-9-]", "");
            score = raw.isBlank() ? 70 : Integer.parseInt(raw);
        }
        return Math.max(0, Math.min(100, score));
    }

    private String buildUserPrompt(EventActivityRequest request) {
        return "Moderer cet evenement et evaluer son niveau de qualite/realisme pour publication. " +
                "Applique ces criteres: titre descriptif, description detaillee, contenu non factice, " +
                "ville/coherence locale, prix coherent. " +
                "Si risque eleve de contenu fake ou insuffisant -> approved=false.\n" +
                "Titre: " + safe(request.getTitle()) + "\n" +
                "Description: " + safe(request.getDescription()) + "\n" +
                "Ville: " + safe(request.getCity()) + "\n" +
                "Adresse: " + safe(request.getAddress()) + "\n" +
                "Type: " + safe(request.getType()) + "\n" +
                "Prix: " + String.valueOf(request.getPrice()) + "\n" +
                "Capacite: " + String.valueOf(request.getCapacity()) + "\n" +
                "Reponds strictement en JSON.";
    }

    private String safe(String value) {
        return value == null ? "" : value.replace('\n', ' ').trim();
    }

    public record ModerationDecision(boolean approved, int score, String reason) {
    }
}
