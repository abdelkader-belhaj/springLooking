package tn.hypercloud.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import tn.hypercloud.payload.response.face.ExtractEmbeddingResponse;
import tn.hypercloud.payload.response.face.VerifyFaceResponse;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class FaceAiClientService {

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String baseUrl;

    public FaceAiClientService(
            ObjectMapper objectMapper,
            @Value("${face.ai.base-url:http://localhost:8001}") String baseUrl
    ) {
        this.httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .build();
        this.objectMapper = objectMapper;
        this.baseUrl = baseUrl;
    }

    public ExtractEmbeddingResponse extractEmbedding(String imageBase64) {
        Map<String, Object> request = new HashMap<>();
        request.put("image_base64", imageBase64);
        request.put("detector_backend", "opencv");
        request.put("model_name", "SFace");

        try {
            String requestJson = objectMapper.writeValueAsString(request);
            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/v1/face/extract-embedding"))
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestJson))
                    .build();

            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 400) {
                throw new RuntimeException("Erreur service IA (extract): " + response.body());
            }

            ExtractEmbeddingResponse body = objectMapper.readValue(response.body(), ExtractEmbeddingResponse.class);
            if (body == null || body.getEmbedding() == null || body.getEmbedding().isEmpty()) {
                throw new RuntimeException("Le service IA n a pas retourne d embedding");
            }

            return body;
        } catch (JsonProcessingException ex) {
            throw new RuntimeException("Erreur serialization JSON (extract): " + ex.getMessage());
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Service IA indisponible (extract): " + ex.getMessage());
        } catch (IOException ex) {
            throw new RuntimeException("Service IA indisponible (extract): " + ex.getMessage());
        }
    }

    public VerifyFaceResponse verifyFace(
            String imageBase64,
            List<Double> enrolledEmbedding,
            double threshold,
            String detectorBackend,
            String modelName
    ) {
        Map<String, Object> request = new HashMap<>();
        request.put("image_base64", imageBase64);
        request.put("enrolled_embedding", enrolledEmbedding);
        request.put("threshold", threshold);
        request.put("detector_backend", detectorBackend);
        request.put("model_name", modelName);

        try {
            String requestJson = objectMapper.writeValueAsString(request);
            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/v1/face/verify"))
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestJson))
                    .build();

            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 400) {
                throw new RuntimeException("Erreur service IA (verify): " + response.body());
            }

            VerifyFaceResponse body = objectMapper.readValue(response.body(), VerifyFaceResponse.class);
            if (body == null || body.getMatched() == null) {
                throw new RuntimeException("Le service IA n a pas retourne de resultat de verification");
            }

            return body;
        } catch (JsonProcessingException ex) {
            throw new RuntimeException("Erreur serialization JSON (verify): " + ex.getMessage());
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Service IA indisponible (verify): " + ex.getMessage());
        } catch (IOException ex) {
            throw new RuntimeException("Service IA indisponible (verify): " + ex.getMessage());
        }
    }
}