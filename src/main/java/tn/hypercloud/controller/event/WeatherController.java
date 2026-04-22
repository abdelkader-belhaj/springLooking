package tn.hypercloud.controller.event;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import tn.hypercloud.service.event.EventActivityService;
import tn.hypercloud.service.event.WeatherService;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/events")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:4200")
public class WeatherController {

    private final WeatherService weatherService;
    private final EventActivityService eventActivityService;

    @GetMapping("/{id}/weather")
    @PreAuthorize("permitAll()")
    public ResponseEntity<WeatherService.WeatherData> getEventWeather(
            @PathVariable Integer id) {
        
        var event = eventActivityService.getPublishedById(id);
        
        System.out.println("=== WEATHER DEBUG ===");
        System.out.println("Event ID: " + id);
        System.out.println("City: " + event.getCity());
        System.out.println("Address: " + event.getAddress());
        System.out.println("Latitude: " + event.getLatitude());
        System.out.println("Longitude: " + event.getLongitude());
        
        // Utiliser lat/lng si disponibles
        if (event.getLatitude() != null && event.getLongitude() != null) {
            System.out.println("Using lat/lng method");
            return ResponseEntity.ok(
                weatherService.getWeather(event.getLatitude(), event.getLongitude())
            );
        }
        
        // Sinon utiliser ville + adresse (geocoding automatique)
        if (event.getCity() != null && !event.getCity().isBlank()) {
            System.out.println("Using city method: " + event.getCity());
            return ResponseEntity.ok(
                weatherService.getWeatherByCityAndAddress(event.getCity(), event.getAddress())
            );
        }

        System.out.println("Using default weather");
        return ResponseEntity.ok(weatherService.getDefaultWeather());
    }

    @GetMapping("/weather/by-city")
    @PreAuthorize("permitAll()")
    public ResponseEntity<WeatherService.WeatherData> getWeatherByCity(
            @RequestParam String city) {
        return ResponseEntity.ok(weatherService.getWeatherByCity(city));
    }

    @GetMapping("/weather/batch")
    @PreAuthorize("permitAll()")
    public ResponseEntity<List<Map<String, Object>>> getWeatherForEvents(
            @RequestParam List<Integer> eventIds) {
        
        List<Map<String, Object>> results = eventIds.stream()
            .map(id -> {
                try {
                    var event = eventActivityService.getPublishedById(id);
                    WeatherService.WeatherData weather = null;
                    
                    if (event.getLatitude() != null && event.getLongitude() != null) {
                        weather = weatherService.getWeather(
                            event.getLatitude(), event.getLongitude());
                    }
                    
                    return Map.<String, Object>of(
                        "eventId", id,
                        "eventTitle", event.getTitle() != null ? 
                            event.getTitle() : "Inconnu",
                        "weather", weather != null ? weather : 
                            weatherService.getDefaultWeather()
                    );
                } catch (Exception e) {
                    return Map.<String, Object>of(
                        "eventId", id,
                        "eventTitle", "Erreur",
                        "weather", weatherService.getDefaultWeather()
                    );
                }
            })
            .toList();

        return ResponseEntity.ok(results);
    }

    private WeatherService.WeatherData getDefaultWeather() {
        return WeatherService.WeatherData.builder()
                .temperature(java.math.BigDecimal.valueOf(25))
                .feelsLike(java.math.BigDecimal.valueOf(24))
                .humidity(60)
                .description("Informations meteo non disponibles")
                .icon("01d")
                .windSpeed(java.math.BigDecimal.ZERO)
                .city("Indisponible")
                .country("")
                .retrievedAt(java.time.LocalDateTime.now())
                .build();
    }
}
