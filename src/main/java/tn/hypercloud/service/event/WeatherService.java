package tn.hypercloud.service.event;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Scanner;

@Service
@Slf4j
public class WeatherService {

    @Value("${weather.api.key}")
    private String apiKey;

    private static final String BASE_URL = "https://api.openweathermap.org/data/2.5/weather";
    private static final String UNITS = "metric";
    private static final String LANG = "fr";

    public WeatherData getWeatherByCityAndAddress(String city, String address) {
        try {
            String location = city + ", Tunisia";
            if (address != null && !address.isBlank()) {
                location = city + ", Tunisia";
            }
            
            String urlString = String.format(
                "%s?q=%s&appid=%s&units=%s&lang=%s",
                BASE_URL, 
                URLEncoder.encode(location, "UTF-8"),
                apiKey, UNITS, LANG
            );
            log.info("Fetching weather with URL: {}", urlString);
            return fetchWeather(urlString);
        } catch (Exception e) {
            log.error("Weather by address error: {}", e.getMessage());
            return getDefaultWeather();
        }
    }

    public WeatherData getWeatherByCity(String city) {
        try {
            String urlString = String.format(
                "%s?q=%s,Tunisia&appid=%s&units=%s&lang=%s",
                BASE_URL, 
                URLEncoder.encode(city, "UTF-8"),
                apiKey, UNITS, LANG
            );
            log.info("Fetching weather with URL: {}", urlString);
            return fetchWeather(urlString);
        } catch (Exception e) {
            log.error("Geocoding error: {}", e.getMessage());
            return getDefaultWeather();
        }
    }

    public WeatherData getWeather(double latitude, double longitude) {
        try {
            String urlString = String.format(
                "%s?lat=%s&lon=%s&appid=%s&units=%s&lang=%s",
                BASE_URL, latitude, longitude, apiKey, UNITS, LANG
            );
            return fetchWeather(urlString);
        } catch (Exception e) {
            log.error("Error fetching weather: {}", e.getMessage());
            return getDefaultWeather();
        }
    }

    private WeatherData fetchWeather(String urlString) throws Exception {
        URL url = new URL(urlString);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.setConnectTimeout(5000);
        connection.setReadTimeout(5000);

        int responseCode = connection.getResponseCode();

        if (responseCode == 200) {
            try (Scanner scanner = new Scanner(connection.getInputStream())) {
                String response = scanner.useDelimiter("\\A").next();
                return parseWeatherResponse(response);
            }
        } else {
            log.warn("Weather API returned code: {}", responseCode);
            return getDefaultWeather();
        }
    }

    private WeatherData parseWeatherResponse(String json) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(json);

            double temp = root.path("main").path("temp").asDouble();
            double feelsLike = root.path("main").path("feels_like").asDouble();
            int humidity = root.path("main").path("humidity").asInt();
            String description = root.path("weather").get(0).path("description").asText();
            String icon = root.path("weather").get(0).path("icon").asText();
            double windSpeed = root.path("wind").path("speed").asDouble();
            String country = root.path("sys").path("country").asText("");
            String city = root.path("name").asText();

            return WeatherData.builder()
                    .temperature(BigDecimal.valueOf(temp))
                    .feelsLike(BigDecimal.valueOf(feelsLike))
                    .humidity(humidity)
                    .description(description)
                    .icon(icon)
                    .windSpeed(BigDecimal.valueOf(windSpeed))
                    .city(city)
                    .country(country)
                    .retrievedAt(LocalDateTime.now())
                    .build();

        } catch (Exception e) {
            log.error("Error parsing weather response: {}", e.getMessage());
            return getDefaultWeather();
        }
    }

    public WeatherData getDefaultWeather() {
        return WeatherData.builder()
                .temperature(BigDecimal.valueOf(25))
                .feelsLike(BigDecimal.valueOf(24))
                .humidity(60)
                .description("Informations meteo non disponibles")
                .icon("01d")
                .windSpeed(BigDecimal.ZERO)
                .city("Indisponible")
                .country("")
                .retrievedAt(LocalDateTime.now())
                .build();
    }

    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class WeatherData {
        private BigDecimal temperature;
        private BigDecimal feelsLike;
        private int humidity;
        private String description;
        private String icon;
        private BigDecimal windSpeed;
        private String city;
        private String country;
        private LocalDateTime retrievedAt;
    }
}
