package tn.hypercloud.service.forum;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.ResponseEntity;
import java.util.Map;

@Service
public class AiService {

    private final RestTemplate restTemplate = new RestTemplate();
    private static final String AI_URL = "http://localhost:5000/analyze";

    public String analyzeSentiment(String text) {
        try {
            Map<String, String> body = Map.of("text", text);
            ResponseEntity<Map> response = restTemplate.postForEntity(AI_URL, body, Map.class);
            return (String) response.getBody().get("sentiment");
        } catch (Exception e) {
            System.err.println("⚠️ AiService error : " + e.getMessage());
            return "NEUTRAL";
        }
    }
}