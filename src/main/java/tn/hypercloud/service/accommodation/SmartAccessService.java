package tn.hypercloud.service.accommodation;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tn.hypercloud.entity.accommodation.Logement;
import tn.hypercloud.payload.request.VerifyLocationRequest;
import tn.hypercloud.payload.response.GeoAccessResponse;
import tn.hypercloud.repository.accommodation.LogementRepository;

@Service
@RequiredArgsConstructor
public class SmartAccessService {

    private final LogementRepository logementRepository;

    private static final double MAX_ALLOWED_DISTANCE_METERS = 50.0;

    public GeoAccessResponse verifyLocationAndUnlock(VerifyLocationRequest request) {
        Logement logement = logementRepository.findById(request.getLogementId())
                .orElseThrow(() -> new RuntimeException("Logement introuvable avec l'ID: " + request.getLogementId()));

        if (logement.getLatitude() == null || logement.getLongitude() == null) {
            return GeoAccessResponse.builder()
                    .success(false)
                    .message("Serrure non configurée : L'hébergeur doit d'abord configurer l'accès géographique.")
                    .build();
        }

        double distance = calculateHaversineDistance(
                request.getClientLatitude(), request.getClientLongitude(),
                logement.getLatitude(), logement.getLongitude()
        );

        if (distance <= MAX_ALLOWED_DISTANCE_METERS) {
            String dynamicCode = generateDynamicUnlockCode(logement.getIdLogement());

            return GeoAccessResponse.builder()
                    .success(true)
                    .message("Succes! Vous êtes sur les lieux. La serrure est déverrouillée!")
                    .distanceMeters(distance)
                    .unlockCode(dynamicCode)
                    .build();
        } else {
            return GeoAccessResponse.builder()
                    .success(false)
                    .message("Erreur: Vous êtes trop loin (" + Math.round(distance) + "m). Rapprochez-vous du logement.")
                    .distanceMeters(distance)
                    .build();
        }
    }

    private double calculateHaversineDistance(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371; 
        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);

        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        double distance = R * c; 
        return distance * 1000; 
    }

    private String generateDynamicUnlockCode(Integer logementId) {
        long timestamp = System.currentTimeMillis() / 10000;
        int code = (int) (Math.abs(logementId * timestamp) % 9000) + 1000;
        return String.valueOf(code);
    }
}