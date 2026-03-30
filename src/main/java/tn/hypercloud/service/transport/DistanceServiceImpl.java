package tn.hypercloud.service.transport;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import tn.hypercloud.entity.transport.Localisation;

@Service
@RequiredArgsConstructor
public class DistanceServiceImpl implements IDistanceService {

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final String OSRM_URL = "http://router.project-osrm.org/route/v1/driving/";

    @Override
    public RouteInfo calculateRoute(Localisation origin, Localisation destination) {
        if (origin == null || destination == null) {
            return new RouteInfo(15.0, 25.0); // fallback
        }

        try {
            String url = String.format("%s%.6f,%.6f;%.6f,%.6f?overview=false&steps=false",
                    OSRM_URL,
                    origin.getLongitude().doubleValue(),
                    origin.getLatitude().doubleValue(),
                    destination.getLongitude().doubleValue(),
                    destination.getLatitude().doubleValue());

            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);

            JsonNode root = objectMapper.readTree(response.getBody());
            JsonNode route = root.path("routes").get(0);

            if (route.isMissingNode()) {
                return new RouteInfo(15.0, 25.0);
            }

            double distanceMeters = route.path("distance").doubleValue();
            double durationSeconds = route.path("duration").doubleValue();

            double distanceKm = distanceMeters / 1000.0;
            double durationMin = durationSeconds / 60.0;

            return new RouteInfo(distanceKm, durationMin);

        } catch (Exception e) {
            // En cas d'erreur (serveur OSRM down, etc.) on retourne une valeur réaliste
            System.err.println("OSRM error: " + e.getMessage());
            return new RouteInfo(15.0, 25.0);
        }
    }
}