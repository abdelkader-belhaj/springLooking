package tn.hypercloud.controller.forum;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import java.net.URI;
import java.util.Map;

@RestController
@RequestMapping("/api/translate")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:4200")
public class TranslationController {

    private final RestTemplate restTemplate = new RestTemplate();

    @GetMapping
    public ResponseEntity<Map<String, String>> translate(
            @RequestParam String text,
            @RequestParam(defaultValue = "fr") String sourceLang,
            @RequestParam(defaultValue = "en") String targetLang) {

        System.out.println("=== TRANSLATION REQUEST ===");
        System.out.println("TEXT: " + text);
        System.out.println("SOURCE: " + sourceLang);
        System.out.println("TARGET: " + targetLang);

        try {
            URI uri = UriComponentsBuilder
                    .fromHttpUrl("https://api.mymemory.translated.net/get")
                    .queryParam("q", text)
                    .queryParam("langpair", sourceLang + "|" + targetLang)
                    .queryParam("mt", "1")
                    .queryParam("onlyprivate", "0")
                    .build()
                    .encode()
                    .toUri();

            System.out.println("URL: " + uri.toString());

            Map response = restTemplate.getForObject(uri, Map.class);
            Map responseData = (Map) response.get("responseData");
            String translated = (String) responseData.get("translatedText");

            System.out.println("TRANSLATED: " + translated);
            System.out.println("===========================");

            return ResponseEntity.ok(Map.of("translated", translated));

        } catch (Exception e) {
            System.out.println("ERREUR: " + e.getMessage());
            return ResponseEntity.ok(Map.of("translated", text));
        }
    }
}