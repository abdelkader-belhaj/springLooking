package tn.hypercloud.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.util.*;

@RestController
@RequestMapping("/api/gemini")
@CrossOrigin(origins = "http://localhost:4200")
public class GeminiController {

    private static final Logger log = LoggerFactory.getLogger(GeminiController.class);

    @Value("${gemini.api.key:}")
    private String geminiApiKey;

    private final RestTemplate rest = new RestTemplate();

    @PostMapping("/generate")
    public ResponseEntity<Map<String,Object>> generate(@RequestBody(required = false) Map<String,Object> body) {
        log.debug("/api/gemini/generate called with body={}", body == null ? "<empty>" : body);

        if (geminiApiKey == null || geminiApiKey.isBlank()) {
            log.warn("Gemini API key not configured");
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Gemini API key not configured on server");
        }

        if (body == null || body.isEmpty()) {
            log.warn("Empty request body");
            return ResponseEntity.badRequest().body(Map.of("error", "Empty request body"));
        }

        String text = null;
        try {
            Object t = body.get("text");
            if (t != null) text = t.toString();
        } catch (Exception ex) {
            log.warn("Error reading 'text' field: {}", ex.getMessage());
        }
        if (text == null || text.isBlank()) {
            log.warn("Missing 'text' field in request body");
            return ResponseEntity.badRequest().body(Map.of("error", "Missing 'text' field"));
        }

        String prompt = "Corrige les fautes dans cette description de logement tunisien.\nFautes à corriger: orthographe, grammaire, syntaxe.\nAméliore le style professionnel.\nGarde toutes les informations originales.\n\nTexte: \"" + text + "\"\n\nCorrection:";

        Map<String,Object> payload = new HashMap<>();
        Map<String,Object> part = Map.of("text", prompt);
        Map<String,Object> contentEntry = Map.of("parts", List.of(part));
        payload.put("contents", List.of(contentEntry));
        payload.put("generationConfig", Map.of("temperature", 0.1, "maxOutputTokens", 1024));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String,Object>> req = new HttpEntity<>(payload, headers);

        String url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=" + geminiApiKey;

        ResponseEntity<Map> resp;
        try {
            resp = rest.postForEntity(url, req, Map.class);
        } catch (HttpClientErrorException | HttpServerErrorException httpEx) {
            log.error("Gemini API returned error: status={}, body={}", httpEx.getStatusCode(), httpEx.getResponseBodyAsString());
            return ResponseEntity.status(httpEx.getStatusCode()).body(Map.of("error", "Gemini API error", "details", httpEx.getResponseBodyAsString()));
        } catch (Exception ex) {
            log.error("Error calling Gemini API: {}", ex.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(Map.of("error", "Error calling Gemini API", "details", ex.getMessage()));
        }

        Map raw = resp.getBody();
        String corrected = null;
        try {
            List candidates = (List) raw.get("candidates");
            if (candidates != null && !candidates.isEmpty()) {
                Map first = (Map) candidates.get(0);
                Map content = (Map) first.get("content");
                List parts = (List) content.get("parts");
                if (parts != null && !parts.isEmpty()) {
                    Map p = (Map) parts.get(0);
                    corrected = (String) p.get("text");
                }
            }
        } catch (Exception e) {
            log.warn("Failed to parse Gemini response: {}", e.getMessage());
        }

        Map<String,Object> out = new HashMap<>();
        out.put("corrected", corrected);
        out.put("raw", raw);
        return ResponseEntity.ok(out);
    }

}
