package tn.hypercloud.service.accommodation;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Service pour effectuer un reverse geocoding:
 * Convertir (latitude, longitude) → (adresse, ville)
 * 
 * Utilise l'API GRATUITE Nominatim (OpenStreetMap)
 * Utilise HttpClient standard (pas de RestTemplate)
 */
@Service
@Slf4j
public class GeolocationService {

    private static final String NOMINATIM_URL = "https://nominatim.openstreetmap.org/reverse";
    private static final int TIMEOUT_SECONDS = 5;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public GeolocationService() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(TIMEOUT_SECONDS))
                .build();
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Reverse geocoding: obtenir adresse/ville depuis lat/lon
     *
     * @param latitude  Latitude (ex: 36.8065)
     * @param longitude Longitude (ex: 10.1966)
     * @return Map avec "address" et "city"
     */
    public Map<String, String> reverseGeocode(Double latitude, Double longitude) {

        log.info("====== REVERSE_GEOCODE_START ======");
        log.info("Parametres: lat={}, lon={}", latitude, longitude);

        Map<String, String> result = new HashMap<>();
        result.put("address", "");
        result.put("city", "");

        // Validation entrée
        if (latitude == null || longitude == null) {
            log.warn("ERREUR: Latitude ou Longitude NULL");
            return result;
        }

        if (latitude < -90 || latitude > 90 || longitude < -180 || longitude > 180) {
            log.warn("ERREUR: Coordonnees invalides: lat={}, lon={}", latitude, longitude);
            return result;
        }

        try {
            log.info("🌍 Reverse geocoding: lat={}, lon={}", latitude, longitude);

            // Construire l'URL Nominatim avec paramètre de langue FRANÇAIS
            String url = String.format(
                    "%s?format=json&lat=%f&lon=%f&zoom=18&addressdetails=1&accept-language=fr",
                    NOMINATIM_URL, latitude, longitude
            );

            log.info("📍 URL Nominatim: {}", url);

            // Créer la requête HTTP
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(new URI(url))
                    .timeout(Duration.ofSeconds(TIMEOUT_SECONDS))
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36")
                    .GET()
                    .build();

            // Envoyer la requête
            log.info("📤 Envoi de la requête à Nominatim...");
            HttpResponse<String> response = httpClient.send(request,
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

            log.info("📥 Réponse HTTP Status: {}", response.statusCode());

            if (response.statusCode() == 200 && response.body() != null && !response.body().isEmpty()) {

                log.info("LOG_API: Status 200 - Body length: {}", response.body().length());
                log.info("LOG_RESPONSE_START: {}", response.body().substring(0, Math.min(200, response.body().length())));

                JsonNode json = objectMapper.readTree(response.body());

                // Extraire l'adresse complète
                String fullAddress = json.path("display_name").asText("");

                // Extraire les détails d'adresse
                JsonNode address = json.path("address");

                log.info("🔍 Détails d'adresse JSON: {}", address.toString());

                // Essayer d'obtenir la ville (priorité: city > town > municipality > county > state > village > suburb)
                String city = address.path("city").asText("");
                if (city.isEmpty()) {
                    city = address.path("town").asText("");
                }
                if (city.isEmpty()) {
                    city = address.path("municipality").asText("");
                }
                if (city.isEmpty()) {
                    city = address.path("county").asText("");
                }
                if (city.isEmpty()) {
                    city = address.path("state").asText("");
                }
                if (city.isEmpty()) {
                    city = address.path("village").asText("");
                }
                if (city.isEmpty()) {
                    city = address.path("suburb").asText("");
                }

                // Extraire le pays
                String country = address.path("country").asText("");

                // Formater: "Ville, Pays"
                if (!city.isEmpty() && !country.isEmpty()) {
                    city = city + ", " + country;
                } else if (country.isEmpty() && !city.isEmpty()) {
                    // Si pas de pays, utiliser seulement la ville
                    city = city;
                } else if (city.isEmpty() && !country.isEmpty()) {
                    // Si pas de ville, utiliser le pays
                    city = country;
                }

                // Construire l'adresse : rue + numéro
                StringBuilder addressBuilder = new StringBuilder();

                String houseNumber = address.path("house_number").asText("");
                String road = address.path("road").asText("");
                String street = address.path("street").asText("");

                if (!houseNumber.isEmpty()) {
                    addressBuilder.append(houseNumber).append(" ");
                }
                if (!road.isEmpty()) {
                    addressBuilder.append(road);
                } else if (!street.isEmpty()) {
                    addressBuilder.append(street);
                }

                String parsedAddress = addressBuilder.toString().trim();
                if (parsedAddress.isEmpty()) {
                    // Fallback: utiliser les 50 premiers caractères de display_name
                    parsedAddress = fullAddress.split(",")[0];
                }

                result.put("address", parsedAddress);
                result.put("city", city);

                log.info("SUCCES: Reverse geocoding OK");
                log.info("VILLE_FINALE: {}", city);
                log.info("ADRESSE_FINALE: {}", parsedAddress);
                log.info("====== REVERSE_GEOCODE_END ======");

                return result;

            } else {
                log.error("ERREUR_STATUS: API Nominatim retourna statut {}", response.statusCode());
                if (response.body() != null && !response.body().isEmpty()) {
                    log.error("ERREUR_BODY: {}", response.body().substring(0, Math.min(500, response.body().length())));
                } else {
                    log.error("ERREUR_BODY: Body vide ou null");
                }
                return result;
            }

        } catch (java.net.ConnectException e) {
            log.error("ERREUR CONNECT: Impossible de se connecter à Nominatim: {}", e.getMessage(), e);
            return result;
        } catch (java.net.SocketTimeoutException e) {
            log.error("ERREUR TIMEOUT: Timeout Nominatim: {}", e.getMessage(), e);
            return result;
        } catch (javax.net.ssl.SSLException e) {
            log.error("ERREUR SSL: Probleme de certificat SSL: {}", e.getMessage(), e);
            return result;
        } catch (Exception e) {
            log.error("ERREUR GENERALE: {} - {}", e.getClass().getName(), e.getMessage(), e);
            return result;
        }
    }

    /**
     * Variante: Geocoding direct (adresse → lat/lon)
     * Utile si besoin ultérieur
     */
    public Map<String, Double> geocode(String address) {

        Map<String, Double> result = new HashMap<>();
        result.put("latitude", 0.0);
        result.put("longitude", 0.0);

        if (address == null || address.isEmpty()) {
            log.warn("❌ Adresse vide");
            return result;
        }

        try {
            log.info("📍 Geocoding: {}", address);

            String encodedAddress = URLEncoder.encode(address, StandardCharsets.UTF_8);
            String url = String.format(
                    "https://nominatim.openstreetmap.org/search?format=json&q=%s&limit=1",
                    encodedAddress
            );

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(new URI(url))
                    .timeout(Duration.ofSeconds(TIMEOUT_SECONDS))
                    .header("User-Agent", "TunisiaTour-Geocoding/1.0")
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request,
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

            if (response.statusCode() == 200 && response.body() != null && !response.body().isEmpty()) {

                JsonNode json = objectMapper.readTree(response.body());

                if (json.isArray() && json.size() > 0) {
                    JsonNode firstResult = json.get(0);
                    Double lat = firstResult.path("lat").asDouble();
                    Double lon = firstResult.path("lon").asDouble();

                    result.put("latitude", lat);
                    result.put("longitude", lon);

                    log.info("✅ Geocoding réussi: lat={}, lon={}", lat, lon);
                    return result;
                }

            }

            log.warn("⚠️ Aucun résultat de geocoding pour: {}", address);
            return result;

        } catch (Exception e) {
            log.error("ERREUR_GENERALE: {} - {}", e.getClass().getName(), e.getMessage(), e);
            return result;
        }

    }
}