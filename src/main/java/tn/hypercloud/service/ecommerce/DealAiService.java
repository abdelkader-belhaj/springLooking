package tn.hypercloud.service.ecommerce;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import tn.hypercloud.dto.ecommerce.DealAiGenerateResponse;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class DealAiService {

    @Value("${ai.groq.api-key}")
    private String groqApiKey;

    @Value("${ai.huggingface.api-key}")
    private String hfApiKey;

    // Match your existing upload path pattern
    @Value("${file.upload-dir:uploads}")
    private String uploadDir;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final String GROQ_URL = "https://api.groq.com/openai/v1/chat/completions";
    private static final String HF_IMAGE_URL = "https://router.huggingface.co/hf-inference/models/black-forest-labs/FLUX.1-schnell";

    public DealAiGenerateResponse generate(String userDescription) {
        // Call Groq and HuggingFace in parallel
        CompletableFuture<Map<String, String>> textFuture = CompletableFuture
                .supplyAsync(() -> callGroq(userDescription));

        CompletableFuture<String> imageFuture = CompletableFuture
                .supplyAsync(() -> generateAndSaveImage(userDescription));

        Map<String, String> fields = textFuture.join();
        String imageUrl = imageFuture.join();

        return DealAiGenerateResponse.builder()
                .title(fields.get("title"))
                .description(fields.get("description"))
                .location(fields.get("location"))
                .region(fields.get("region"))
                .budget(fields.get("budget"))
                .activityType(fields.get("activityType"))
                .environment(fields.get("environment"))
                .category(fields.get("category"))
                .duration(fields.get("duration"))
                .imageUrl(imageUrl)
                .build();
    }

    @SuppressWarnings("unchecked")
    private Map<String, String> callGroq(String description) {
        String systemPrompt = """
                You are an assistant that fills deal forms for a Tunisian activity platform.
                Given a description, return ONLY a valid JSON object with NO markdown, NO extra text.
                Use exactly these keys and allowed values:
                - title: short catchy title (max 60 chars)
                - description: improved marketing description (2-3 sentences in the same language as the input)
                - location: specific Tunisian city or place
                - region: one of [north, south, center, east_coast, sahara]
                - budget: one of [low, medium, high]
                - activityType: one of [solo, duo, group, flexible]
                - environment: one of [indoor, outdoor, both]
                - category: one of [adventure, culture_history, food, relaxation, water_sports, crafts, nature_hiking, heritage, photography]
                - duration: one of [one_hour, two_hours, three_hours, half_day, full_day, two_days, three_days_plus, weekend]
                Return ONLY the JSON object, nothing else.
                """;

        Map<String, Object> body = Map.of(
                "model", "llama-3.1-8b-instant",
                "temperature", 0.3,
                "messages", List.of(
                        Map.of("role", "system", "content", systemPrompt),
                        Map.of("role", "user", "content", description)));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(groqApiKey);

        try {
            String bodyJson = objectMapper.writeValueAsString(body);
            ResponseEntity<String> response = restTemplate.exchange(
                    GROQ_URL, HttpMethod.POST,
                    new HttpEntity<>(bodyJson, headers),
                    String.class);
            JsonNode root = objectMapper.readTree(response.getBody());
            String content = root.at("/choices/0/message/content").asText();

            // Strip markdown code fences if model adds them anyway
            content = content.replaceAll("```json", "").replaceAll("```", "").trim();

            return objectMapper.readValue(content, Map.class);
        } catch (Exception e) {
            log.error("Groq API call failed: {}", e.getMessage());
            throw new RuntimeException("AI text generation failed: " + e.getMessage(), e);
        }
    }

    private String generateAndSaveImage(String description) {
        String imagePrompt = "Professional promotional photo for a Tunisian tourism activity: "
                + description
                + ". Vibrant colors, warm natural lighting, photorealistic, high quality.";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(hfApiKey);
        // ✅ ADD THIS — tell HuggingFace we want an image back
        headers.setAccept(List.of(MediaType.IMAGE_JPEG));

        Map<String, Object> body = Map.of("inputs", imagePrompt);

        try {
            String bodyJson = objectMapper.writeValueAsString(body);
            ResponseEntity<byte[]> response = restTemplate.exchange(
                    HF_IMAGE_URL, HttpMethod.POST,
                    new HttpEntity<>(bodyJson, headers),
                    byte[].class);

            byte[] imageBytes = response.getBody();
            if (imageBytes == null || imageBytes.length == 0) {
                log.warn("HuggingFace returned empty image");
                return null;
            }

            return saveImageToDisk(imageBytes);

        } catch (Exception e) {
            log.error("HuggingFace image generation failed: {}", e.getMessage());
            return null;
        }
    }

    private String saveImageToDisk(byte[] imageBytes) throws IOException {
        // Same pattern as your existing FileUploadService for deals
        Path dealsDir = Paths.get(uploadDir, "deals");
        Files.createDirectories(dealsDir);

        String filename = UUID.randomUUID() + "_ai_generated.jpg";
        Path filePath = dealsDir.resolve(filename);
        Files.write(filePath, imageBytes);

        return "/uploads/deals/" + filename;
    }
}
