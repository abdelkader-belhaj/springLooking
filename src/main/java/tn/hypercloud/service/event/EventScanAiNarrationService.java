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

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class EventScanAiNarrationService {

    private static final DateTimeFormatter HH_MM = DateTimeFormatter.ofPattern("HH:mm");

    @Value("${moderation.ai.api.url:${groq.api.url:https://api.groq.com/openai/v1/chat/completions}}")
    private String apiUrl;

    @Value("${moderation.ai.api.key:${groq.api.key:}}")
    private String apiKey;

    @Value("${moderation.ai.model:qwen3-32b}")
    private String model;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public String buildScanMessage(ScanNarrationContext ctx) {
        String fallback = buildFallbackMessage(ctx);

        if (apiKey == null || apiKey.isBlank()) {
            return fallback;
        }

        try {
            String prompt = buildPrompt(ctx);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);

            Map<String, Object> payload = Map.of(
                    "model", normalizeModel(model),
                    "temperature", 0.2,
                    "max_tokens", 120,
                    "messages", List.of(
                            Map.of("role", "system", "content",
                                            "Tu es un assistant de contrôle d'accès événementiel. Réponds en français, ton pro, 1 à 2 phrases max. N'écris jamais de balises <think>, ni d'explication, ni de titre. Réponds uniquement avec la phrase finale.") ,
                            Map.of("role", "user", "content", prompt)
                    )
            );

            ResponseEntity<String> response = restTemplate.postForEntity(
                    apiUrl,
                    new HttpEntity<>(payload, headers),
                    String.class
            );

            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                return fallback;
            }

            JsonNode root = objectMapper.readTree(response.getBody());
            String content = root.path("choices").path(0).path("message").path("content").asText("").trim();
            content = sanitizeContent(content);
            if (content.isEmpty()) {
                return fallback;
            }
            return content;
        } catch (Exception ex) {
            log.debug("Scan AI narration fallback used: {}", ex.getMessage());
            return fallback;
        }
    }

    private String buildPrompt(ScanNarrationContext ctx) {
        return "Contexte scan ticket:\n" +
                "- Client: " + safe(ctx.ownerName()) + "\n" +
                "- Événement: " + safe(ctx.eventTitle()) + "\n" +
                "- TicketCode: " + safe(ctx.ticketCode()) + "\n" +
                "- Heure scan actuelle: " + formatTime(ctx.scanTime()) + "\n" +
                "- Ticket déjà utilisé: " + ctx.alreadyUsed() + "\n" +
                "- Première utilisation: " + ctx.firstUse() + "\n" +
                "- Heure première utilisation: " + formatTime(ctx.firstUsedAt()) + "\n" +
                "- Scanné par: " + safe(ctx.scannedBy()) + "\n" +
            "Instruction: si firstUse=true => répond exactement comme: Ticket valide ! [Client] est autorisée à accéder à [Événement]. Bonne visite ! 🎉 " +
            "Si alreadyUsed=true => répond exactement comme: Accès refusé. Ce ticket a déjà été scanné à [HH:mm]. Veuillez contacter l'organisateur.";
    }

    private String buildFallbackMessage(ScanNarrationContext ctx) {
        if (ctx.firstUse()) {
            return "Ticket valide ! " + safe(ctx.ownerName()) +
                " est autorisée à accéder à " + safe(ctx.eventTitle()) +
                ". Première utilisation à " + formatTime(ctx.scanTime()) + ". Bonne visite ! 🎉";
        }

        if (ctx.alreadyUsed()) {
            String by = safe(ctx.scannedBy()).isBlank() ? "organisateur" : safe(ctx.scannedBy());
            return "Accès refusé. Ce ticket a déjà été scanné à " + formatTime(ctx.firstUsedAt()) +
                " par " + by + ".";
        }

        return "Ticket valide ✅";
    }

    private String normalizeModel(String rawModel) {
        if (rawModel == null || rawModel.isBlank()) {
            return "qwen/qwen3-32b";
        }
        if ("qwen3-32b".equalsIgnoreCase(rawModel.trim())) {
            return "qwen/qwen3-32b";
        }
        return rawModel.trim();
    }

    private String formatTime(LocalDateTime dateTime) {
        if (dateTime == null) {
            return "--:--";
        }
        return dateTime.format(HH_MM);
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private String sanitizeContent(String content) {
        if (content == null || content.isBlank()) {
            return "";
        }

        String cleaned = content
                .replaceAll("(?is)<think>.*?</think>", "")
                .replaceAll("(?im)^\\s*<think>.*$", "")
                .replaceAll("(?im)^\\s*Ce que Qwen3 génère\s*[:：]?\\s*", "")
                .replaceAll("(?im)^\\s*(✅|❌)?\\s*(Valide|Déjà utilisé|Deja utilise|Déjà Utilisé)\\s*[:：]?\\s*", "")
                .replaceAll("(?im)^\\s*\"", "")
                .replaceAll("(?im)\"\\s*$", "")
                .trim();

        if (cleaned.isEmpty()) {
            return "";
        }

        return cleaned;
    }

    public record ScanNarrationContext(
            boolean firstUse,
            boolean alreadyUsed,
            String ownerName,
            String eventTitle,
            String ticketCode,
            String scannedBy,
            LocalDateTime scanTime,
            LocalDateTime firstUsedAt
    ) {
    }
}
