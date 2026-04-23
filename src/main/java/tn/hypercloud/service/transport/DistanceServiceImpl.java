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
    private static final double FALLBACK_SPEED_KMH = 28.0;
    private static final double FALLBACK_MIN_MINUTES = 3.0;
    private static final double FALLBACK_EXTRA_MINUTES = 3.0;

    @Override
    public RouteInfo calculateRoute(Localisation origin, Localisation destination) {
        if (!isValid(origin) || !isValid(destination)) {
            return new RouteInfo(0.0, FALLBACK_MIN_MINUTES);
        }

        try {
            String url = String.format(
                    "%s%.6f,%.6f;%.6f,%.6f?overview=false&steps=false",
                    OSRM_URL,
                    origin.getLongitude().doubleValue(),
                    origin.getLatitude().doubleValue(),
                    destination.getLongitude().doubleValue(),
                    destination.getLatitude().doubleValue()
            );

            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            JsonNode root = objectMapper.readTree(response.getBody());
            JsonNode route = root.path("routes").get(0);

            if (route == null || route.isMissingNode()) {
                return fallbackRoute(origin, destination);
            }

            double distanceMeters = route.path("distance").asDouble(0.0);
            double durationSeconds = route.path("duration").asDouble(0.0);

            if (distanceMeters <= 0.0 || durationSeconds <= 0.0) {
                return fallbackRoute(origin, destination);
            }

            double distanceKm = distanceMeters / 1000.0;
            double durationMin = durationSeconds / 60.0;

            return new RouteInfo(
                    round2(distanceKm),
                    round2(Math.max(FALLBACK_MIN_MINUTES, durationMin))
            );

        } catch (Exception e) {
            System.err.println("OSRM error, fallback dynamic route: " + e.getMessage());
            return fallbackRoute(origin, destination);
        }
    }

    private RouteInfo fallbackRoute(Localisation origin, Localisation destination) {
        double distanceKm = haversineKm(
                origin.getLatitude().doubleValue(),
                origin.getLongitude().doubleValue(),
                destination.getLatitude().doubleValue(),
                destination.getLongitude().doubleValue()
        );

        double durationMin = Math.max(
                FALLBACK_MIN_MINUTES,
                (distanceKm / FALLBACK_SPEED_KMH) * 60.0 + FALLBACK_EXTRA_MINUTES
        );

        return new RouteInfo(round2(distanceKm), round2(durationMin));
    }

    private boolean isValid(Localisation l) {
        return l != null && l.getLatitude() != null && l.getLongitude() != null;
    }

    private double haversineKm(double lat1, double lon1, double lat2, double lon2) {
        final double R = 6371.0;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }

    private double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}