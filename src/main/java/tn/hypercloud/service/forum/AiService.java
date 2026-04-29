package tn.hypercloud.service.forum;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import java.util.Map;

@Service
public class AiService {

    private final RestTemplate restTemplate = new RestTemplate();
    private static final String AI_URL = "http://localhost:5000/analyze";

    public String analyzeSentiment(String text) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);  // ← sets Content-Type: application/json

            Map<String, String> body = Map.of("text", text);
            HttpEntity<Map<String, String>> request = new HttpEntity<>(body, headers);  // ← wrap body + headers

            ResponseEntity<Map> response = restTemplate.postForEntity(AI_URL, request, Map.class);
            return (String) response.getBody().get("sentiment");
        } catch (Exception e) {
            System.err.println("⚠️ AiService error : " + e.getMessage());
            return "NEUTRAL";
        }
    }
}