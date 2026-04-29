package tn.hypercloud.service.event;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import tn.hypercloud.dto.event.EventAiPriceSuggestionRequest;
import tn.hypercloud.dto.event.EventAiPriceSuggestionResponse;
import tn.hypercloud.dto.event.EventActivityResponse;
import tn.hypercloud.dto.event.EventAiAssistantResponse;

import java.time.LocalDate;
import java.time.DayOfWeek;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import java.text.Normalizer;

@Service
@RequiredArgsConstructor
public class EventAiAssistantService {

    private final EventActivityService eventActivityService;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    @Value("${farah.groq.api.url:https://api.groq.com/openai/v1/chat/completions}")
    private String groqApiUrl;

    @Value("${farah.groq.api.key:}")
    private String groqApiKey;

    @Value("${farah.groq.model:llama-3.3-70b-versatile}")
    private String groqModel;

    public EventAiPriceSuggestionResponse suggestPrice(EventAiPriceSuggestionRequest request) {
        if (request == null) {
            return EventAiPriceSuggestionResponse.builder()
                    .price(0)
                    .label("Accessible")
                    .rationale("Requête invalide.")
                    .aiUsed(false)
                    .build();
        }

        if (groqApiKey == null || groqApiKey.trim().isEmpty()) {
            return fallbackLocalPrice(request, false, "Service IA non configuré. Estimation provisoire.");
        }

        String prompt = buildPricePrompt(request);
        try {
            String groqResponse = callGroqAPI(prompt);
            EventAiPriceSuggestionResponse parsed = parsePriceResponse(groqResponse);
            if (parsed != null) {
                return normalizePriceResponse(parsed, true);
            }
        } catch (Exception ignored) {
        }

        return fallbackLocalPrice(request, false, "IA indisponible. Estimation de secours.");
    }

    public EventAiAssistantResponse recommend(String userMessage, Integer requestedMaxResults) {
        List<EventActivityResponse> events = eventActivityService.getPublished();
        int maxResults = Math.min(requestedMaxResults != null ? requestedMaxResults : 5, 6);

        if (events.isEmpty()) {
            return fallback("Désolé, aucun événement n'est disponible pour le moment.");
        }

        if (groqApiKey == null || groqApiKey.trim().isEmpty()) {
            return fallbackWithEvents(events, maxResults, "Service IA non configuré. Voici quelques événements disponibles :");
        }

        String prompt = buildStrictPrompt(userMessage, events, maxResults);

        try {
            String groqResponse = callGroqAPI(prompt);
            EventAiAssistantResponse result = parseGroqResponse(groqResponse, events, maxResults);
            if (result != null) {
                if (result.getRecommendedEventIds() == null || result.getRecommendedEventIds().isEmpty()) {
                    List<Integer> localIds = heuristicRecommendations(userMessage, events, maxResults);
                    if (!localIds.isEmpty()) {
                        return EventAiAssistantResponse.builder()
                                .answer(buildHeuristicAnswer(userMessage, localIds.size()))
                                .recommendedEventIds(localIds)
                                .build();
                    }
                }
                return result;
            }
        } catch (Exception ignored) {}

        List<Integer> localIds = heuristicRecommendations(userMessage, events, maxResults);
        if (!localIds.isEmpty()) {
            return EventAiAssistantResponse.builder()
                    .answer(buildHeuristicAnswer(userMessage, localIds.size()))
                    .recommendedEventIds(localIds)
                    .build();
        }

        return fallbackWithEvents(events, maxResults, "Voici les événements disponibles :");
    }

    private String buildStrictPrompt(String userMessage, List<EventActivityResponse> events, int maxResults) {
        String today = LocalDate.now().format(DATE_FORMATTER);

        String eventsText = events.stream()
                .limit(45)
                .map(e -> String.format(
                        "ID:%d | Titre:%s | Ville:%s | Catégorie:%s | Prix:%s TND | Date:%s",
                        e.getId(),
                        safe(e.getTitle()),
                        safe(e.getCity()),
                        safe(e.getCategoryName()),
                        e.getPrice() != null ? e.getPrice() : "Gratuit",
                        e.getStartDate() != null ? e.getStartDate().format(DATE_FORMATTER) : "N/A"
                ))
                .collect(Collectors.joining("\n"));

        return """
                Tu es un assistant de recommandation d'événements en Tunisie.

                DATE D'AUJOURD'HUI : %s
                Demande de l'utilisateur : "%s"

                Événements disponibles :
                %s

                RÈGLES ABSOLUES — NE JAMAIS VIOLER :

                1. BUDGET : Si l'utilisateur mentionne un budget maximum, INTERDIRE tout événement dont le Prix dépasse ce budget. Règle DURE sans exception.

                2. VILLE : Si l'utilisateur mentionne "Tunis", voici la liste EXACTE des villes acceptées :
                   ✅ Tunis, Carthage, Sidi Bou Saïd, Gammarth, Le Lac
                   ❌ Hammamet, Monastir, Sfax, El Jem, Douz, Tabarka, Zaghouan, Sejnane, Bizerte = STRICTEMENT REFUSÉ
                   Si la ville n'est PAS dans la liste acceptée → événement INTERDIT.

                3. THÈME : Analyser le thème demandé et appliquer ces correspondances STRICTES :
                   - "musique" → accepter UNIQUEMENT catégorie Musique ou titre contenant : festival, concert, jazz, symphony, musical
                   - "sport" → accepter UNIQUEMENT catégorie Sport ou titre contenant : marathon, course, match, tournoi
                   - "nature" → accepter UNIQUEMENT catégorie Nature ou titre contenant : randonnée, trek, montagne, forêt
                   - "mer" → accepter UNIQUEMENT titre contenant : plage, mer, voile, plongée, nautique, côte
                   - "art" → accepter UNIQUEMENT catégorie Art ou titre contenant : atelier, poterie, artisanat, peinture
                   - Si plusieurs thèmes → l'événement doit correspondre à AU MOINS UN thème demandé
                   - JAMAIS recommander un événement hors thème même si ville et budget correspondent

                4. DATE : Si l'utilisateur mentionne une date ou "ce weekend" :
                   - Calculer à partir de la DATE D'AUJOURD'HUI (%s)
                   - "ce weekend" = prochain samedi et dimanche
                   - Vérifier que la date de l'événement correspond à la période demandée
                   - Un événement dont la date est APRÈS aujourd'hui est valide
                   - Ne jamais dire "hors date" sans vérifier par rapport à aujourd'hui

                5. AUCUN MATCH : Si aucun événement ne satisfait TOUTES les contraintes :
                   - recommendedEventIds = []
                   - Expliquer honnêtement en 1 phrase sans mentionner les IDs
                   - Ne pas proposer d'alternatives qui violent les contraintes

                6. Maximum %d événements dans la réponse.

                7. RÉPONSE : Concis et naturel en français. Maximum 2 phrases. Pas de répétition. Ne jamais mentionner les IDs.

                Réponds UNIQUEMENT avec ce JSON exact, sans markdown, sans texte avant ou après :
                {
                  "answer": "Réponse concise en français sans mentionner les IDs",
                  "recommendedEventIds": []
                }
                """.formatted(today, userMessage, eventsText, today, maxResults);
    }

        private String buildPricePrompt(EventAiPriceSuggestionRequest request) {
                String today = LocalDate.now().format(DATE_FORMATTER);
                String title = safe(request.getTitle());
                String description = safe(request.getDescription());
                String type = safe(request.getType());
                String category = safe(request.getCategoryName());
                String city = safe(request.getCity());
                String address = safe(request.getAddress());
                String capacity = request.getCapacity() != null ? String.valueOf(request.getCapacity()) : "N/A";
                String startDate = safe(request.getStartDate());
                String endDate = safe(request.getEndDate());

                return """
                                Tu es un expert pricing d'événements en Tunisie.

                                DATE D'AUJOURD'HUI : %s

                                Données de l'événement :
                                - Titre : %s
                                - Description : %s
                                - Type : %s
                                - Catégorie : %s
                                - Ville : %s
                                - Adresse : %s
                                - Capacité : %s
                                - Début : %s
                                - Fin : %s

                                OBJECTIF : proposer un prix réaliste, vendeur et cohérent avec le marché tunisien.

                                RÈGLES :
                                - Réponds uniquement en JSON.
                                - Le prix doit être un entier en TND.
                                - Si le titre ou la description semblent factices, non sérieux, ou sans sens (ex: test, aaa, @#$, chiffres aléatoires, texte sans signification), retourne price = null et explain = 'Contenu insuffisant'.
                                - Sinon retourne un prix logique en TND.
                                - Utilise un prix accessible si l'événement est simple, standard s'il a un bon positionnement, premium si l'expérience est forte.
                                - Si c'est une activité mer / bateau / excursion, le prix doit refléter une prestation réelle, repas ou animation inclus si le texte le suggère.
                                - Privilégie un prix psychologique rond ou proche d'un multiple de 5.

                                Format exact attendu :
                                {
                                    "price": 110,
                                    "label": "Standard",
                                    "explain": "Explication courte et précise en français"
                                }
                                """.formatted(today, title, description, type, category, city, address, capacity, startDate, endDate);
        }

    private String callGroqAPI(String prompt) throws Exception {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(groqApiKey);

        Map<String, Object> payload = Map.of(
                "model", groqModel,
                "temperature", 0.0,
                "max_tokens", 800,
                "messages", List.of(
                        Map.of("role", "system", "content",
                                "Tu es un assistant de recommandation strict pour des événements en Tunisie. " +
                                        "Tu respectes ABSOLUMENT toutes les contraintes (budget, ville, thème, date). " +
                                        "THÈME STRICT : si l'utilisateur demande musique → UNIQUEMENT musique, si sport → UNIQUEMENT sport. " +
                                        "JAMAIS mélanger les thèmes. JAMAIS recommander hors thème. " +
                                        "Tu réponds UNIQUEMENT en JSON valide, sans markdown, sans backticks. " +
                                        "Si aucun match → recommendedEventIds = []. " +
                                        "Ne mentionne JAMAIS les IDs dans answer. Maximum 2 phrases."),
                        Map.of("role", "user", "content", prompt)
                )
        );

        ResponseEntity<String> response = restTemplate.postForEntity(
                groqApiUrl,
                new HttpEntity<>(payload, headers),
                String.class
        );

        JsonNode root = objectMapper.readTree(response.getBody());
        return root.path("choices").path(0).path("message").path("content").asText().trim();
    }

    private EventAiAssistantResponse parseGroqResponse(String rawResponse, List<EventActivityResponse> events, int maxResults) {
        try {
            String cleaned = rawResponse
                    .replaceAll("(?s)```json\\s*", "")
                    .replaceAll("(?s)```\\s*", "")
                    .trim();

            JsonNode aiJson = objectMapper.readTree(cleaned);
            String answer = aiJson.path("answer").asText("Voici mes recommandations :");

            List<Integer> ids = new ArrayList<>();
            JsonNode idsNode = aiJson.path("recommendedEventIds");

            if (idsNode.isArray()) {
                for (JsonNode node : idsNode) {
                    int id = node.asInt();
                    if (events.stream().anyMatch(e -> e.getId() == id)) {
                        ids.add(id);
                    }
                }
            }

            ids = ids.stream().distinct().limit(maxResults).collect(Collectors.toList());

            return EventAiAssistantResponse.builder()
                    .answer(answer)
                    .recommendedEventIds(ids)
                    .build();

        } catch (Exception e) {
            return null;
        }
    }

    private EventAiAssistantResponse fallback(String message) {
        return EventAiAssistantResponse.builder()
                .answer(message)
                .recommendedEventIds(List.of())
                .build();
    }

    private EventAiAssistantResponse fallbackWithEvents(List<EventActivityResponse> events, int maxResults, String reason) {
        List<Integer> ids = events.stream()
                .limit(maxResults)
                .map(EventActivityResponse::getId)
                .collect(Collectors.toList());

        return EventAiAssistantResponse.builder()
                .answer(reason)
                .recommendedEventIds(ids)
                .build();
    }

    private EventAiPriceSuggestionResponse parsePriceResponse(String rawResponse) {
        try {
            String cleaned = rawResponse
                    .replaceAll("(?s)```json\\s*", "")
                    .replaceAll("(?s)```\\s*", "")
                    .trim();

            JsonNode aiJson = objectMapper.readTree(cleaned);
            Integer price = aiJson.hasNonNull("price") ? aiJson.path("price").asInt() : null;
            String label = aiJson.path("label").asText("Standard");
                String rationale = aiJson.hasNonNull("rationale")
                    ? aiJson.path("rationale").asText()
                    : aiJson.path("explain").asText("Estimation basée sur les informations fournies.");

            return EventAiPriceSuggestionResponse.builder()
                    .price(price)
                    .label(label)
                    .rationale(rationale)
                    .aiUsed(true)
                    .build();
        } catch (Exception e) {
            return null;
        }
    }

    private EventAiPriceSuggestionResponse normalizePriceResponse(EventAiPriceSuggestionResponse response, boolean aiUsed) {
        if (response.getPrice() == null) {
            String rationale = response.getRationale();
            if (rationale == null || rationale.trim().isEmpty()) {
                rationale = "Contenu insuffisant";
            }
            return EventAiPriceSuggestionResponse.builder()
                    .price(null)
                    .label(response.getLabel() == null || response.getLabel().trim().isEmpty() ? "Insuffisant" : response.getLabel())
                    .rationale(rationale)
                    .aiUsed(aiUsed)
                    .build();
        }

        int price = response.getPrice();
        price = Math.max(0, Math.min(250, price));
        if (price % 5 != 0) {
            price = Math.round(price / 5.0f) * 5;
        }

        String label = response.getLabel();
        if (label == null || label.trim().isEmpty()) {
            label = price >= 120 ? "Premium" : price >= 70 ? "Standard" : "Accessible";
        }

        String rationale = response.getRationale();
        if (rationale == null || rationale.trim().isEmpty()) {
            rationale = "Estimation IA générée à partir des détails de l'événement.";
        }

        return EventAiPriceSuggestionResponse.builder()
                .price(price)
                .label(label)
                .rationale(rationale)
                .aiUsed(aiUsed)
                .build();
    }

    private EventAiPriceSuggestionResponse fallbackLocalPrice(EventAiPriceSuggestionRequest request, boolean aiUsed, String reason) {
        String text = normalizePriceText(
            safe(request.getTitle()) + " " + safe(request.getDescription()) + " " + safe(request.getCategoryName()) + " " + safe(request.getCity()) + " " + safe(request.getAddress())
        );
        int price = request.getType() != null && request.getType().equalsIgnoreCase("ACTIVITY") ? 35 : 55;

        if (text.matches(".*(mer|plage|nautique|bateau|yacht|plongee).*")) price += 35;
        if (text.matches(".*(festival|concert|jazz|musique|live).*")) price += 30;
        if (text.matches(".*(atelier|conférence|conference|formation|masterclass).*")) price += 15;
        if (request.getCapacity() != null) {
            if (request.getCapacity() >= 150) price += 20;
            else if (request.getCapacity() >= 80) price += 10;
            else if (request.getCapacity() <= 30) price -= 5;
        }

        price = Math.max(0, Math.min(250, Math.round(price / 5.0f) * 5));

        String label = price >= 120 ? "Premium" : price >= 70 ? "Standard" : "Accessible";
        return EventAiPriceSuggestionResponse.builder()
                .price(price)
                .label(label)
                .rationale(reason + " " + buildFallbackRationale(request))
                .aiUsed(aiUsed)
                .build();
    }

    private String buildFallbackRationale(EventAiPriceSuggestionRequest request) {
        List<String> parts = new ArrayList<>();
        if (request.getCategoryName() != null && !request.getCategoryName().isBlank()) parts.add(request.getCategoryName());
        if (request.getCity() != null && !request.getCity().isBlank()) parts.add(request.getCity());
        if (request.getType() != null && !request.getType().isBlank()) parts.add(request.getType().toLowerCase(Locale.ROOT));
        return String.join(" · ", parts);
    }

    private String normalizePriceText(String value) {
        return Normalizer.normalize(value == null ? "" : value.toLowerCase(Locale.ROOT), Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "");
    }

    private String safe(Object value) {
        return value == null ? "" : String.valueOf(value).replace("\n", " ").trim();
    }

    private String safe(String value) {
        return value == null ? "" : value.replace("\n", " ").trim();
    }

    private List<Integer> heuristicRecommendations(String userMessage, List<EventActivityResponse> events, int maxResults) {
        String q = normalize(userMessage);
        boolean wantsWeekend = q.contains("weekend") || q.contains("week-end") || q.contains("ce weekend") || q.contains("ce week-end");

        Map<String, List<String>> themes = Map.of(
                "musique", List.of("musique", "concert", "festival", "jazz", "dj", "live", "soiree"),
                "mer", List.of("mer", "plage", "nautique", "bateau", "yacht", "plongee", "surf", "cote"),
                "sport", List.of("sport", "course", "marathon", "match", "tournoi", "fitness", "yoga"),
                "nature", List.of("nature", "randonnee", "trek", "montagne", "foret", "desert"),
                "art", List.of("art", "artisanat", "atelier", "peinture", "expo", "theatre")
        );

        Set<String> requestedThemes = themes.entrySet().stream()
                .filter(e -> e.getValue().stream().anyMatch(q::contains))
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());

        Set<String> knownCities = events.stream()
                .map(EventActivityResponse::getCity)
                .filter(Objects::nonNull)
                .map(this::normalize)
                .collect(Collectors.toSet());

        Set<String> requestedCities = knownCities.stream()
                .filter(q::contains)
                .collect(Collectors.toSet());

        LocalDate now = LocalDate.now();
        LocalDate saturday = now.with(java.time.temporal.TemporalAdjusters.nextOrSame(DayOfWeek.SATURDAY));
        LocalDate sunday = saturday.plusDays(1);

        List<EventActivityResponse> scored = events.stream()
                .filter(e -> e.getStartDate() != null)
                .filter(e -> {
                    if (requestedCities.isEmpty()) return true;
                    return requestedCities.contains(normalize(e.getCity()));
                })
                .filter(e -> {
                    if (!wantsWeekend) return true;
                    LocalDate d = e.getStartDate().toLocalDate();
                    return !d.isBefore(saturday) && !d.isAfter(sunday);
                })
                .sorted((a, b) -> Integer.compare(scoreEvent(b, requestedThemes, themes), scoreEvent(a, requestedThemes, themes)))
                .collect(Collectors.toList());

        List<Integer> ids = scored.stream()
                .filter(e -> requestedThemes.isEmpty() || scoreEvent(e, requestedThemes, themes) >= 6)
                .map(EventActivityResponse::getId)
                .filter(Objects::nonNull)
                .limit(maxResults)
                .collect(Collectors.toList());

        if (ids.isEmpty()) {
            ids = scored.stream()
                    .map(EventActivityResponse::getId)
                    .filter(Objects::nonNull)
                    .limit(maxResults)
                    .collect(Collectors.toList());
        }

        return ids;
    }

    private int scoreEvent(
            EventActivityResponse event,
            Set<String> requestedThemes,
            Map<String, List<String>> themes
    ) {
        String text = normalize(
                safe(event.getTitle()) + " " + safe(event.getDescription()) + " " + safe(event.getCategoryName()) + " " + safe(event.getCity())
        );
        int score = 0;
        if ("PUBLISHED".equalsIgnoreCase(safe(event.getStatus()))) score += 4;

        if (requestedThemes.isEmpty()) {
            score += 1;
        } else {
            for (String theme : requestedThemes) {
                List<String> keys = themes.getOrDefault(theme, List.of());
                boolean hit = keys.stream().anyMatch(text::contains);
                if (hit) score += 6;
            }
        }

        if (event.getStartDate() != null && !event.getStartDate().toLocalDate().isBefore(LocalDate.now())) {
            score += 2;
        }

        return score;
    }

    private String buildHeuristicAnswer(String userMessage, int count) {
        String q = normalize(userMessage);
        if (q.contains("weekend") || q.contains("week-end")) {
            return "J'ai trouvé " + count + " événement(s) pour ce weekend selon vos thèmes.";
        }
        return "J'ai trouvé " + count + " événement(s) proches de vos préférences.";
    }

    private String normalize(String value) {
        String lower = value == null ? "" : value.toLowerCase(Locale.ROOT);
        String normalized = Normalizer.normalize(lower, Normalizer.Form.NFD);
        return normalized.replaceAll("\\p{M}+", "");
    }
}