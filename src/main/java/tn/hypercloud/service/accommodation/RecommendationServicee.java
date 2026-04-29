package tn.hypercloud.service.accommodation;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.databind.ObjectMapper;

import tn.hypercloud.entity.accommodation.Logement;
import tn.hypercloud.entity.accommodation.UserPreferences;
import tn.hypercloud.payload.response.LogementResponse;
import tn.hypercloud.payload.response.RecommendationResponse;
import tn.hypercloud.repository.accommodation.LogementRepository;
import tn.hypercloud.repository.accommodation.UserPreferencesRepository;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.HashSet;

@Service
@RequiredArgsConstructor
public class RecommendationServicee {

    private final LogementRepository logementRepository;
    private final LogementService logementService;
    private final UserPreferencesRepository prefsRepository;

    // 📋 Équipements standard pour scoring
    private static final Set<String> EQUIPEMENTS_PREMIUM = Set.of(
        "wifi", "climatisation", "piscine", "jacuzzi", "vue mer",
        "parking", "machine à laver", "balcon", "terrasse", "chauffage",
        "ascenseur", "sécurité 24h", "borne électrique", "smart tv",
        "espace travail", "salle de sport", "sauna"
    );

    public List<RecommendationResponse> getRecommandationsPourUser(Integer userId) {
        List<Logement> logements = logementRepository.findByDisponibleTrue();
        UserPreferences prefs = prefsRepository.findByUserId(userId).orElse(null);

        List<ScoredLogement> scored = new ArrayList<>();
        for (Logement l : logements) {
            double score = calculerScore(l, prefs);
            scored.add(new ScoredLogement(l, score));
        }

        scored.sort(Comparator.comparingDouble(ScoredLogement::score).reversed());

        List<RecommendationResponse> result = new ArrayList<>();
        for (ScoredLogement s : scored.subList(0, Math.min(6, scored.size()))) {
            try {
                LogementResponse resp = logementService.toResponse(s.logement());
                result.add(RecommendationResponse.builder()
                        .logement(resp)
                        .aiScore(Math.round(s.score() * 100.0) / 100.0)
                        .build());
            } catch (Exception ignored) {}
        }
        return result;
    }

    private double calculerScore(Logement l, UserPreferences prefs) {
        // 🌟 Score de base = nombre d'équipements présents (0 à 3 étoiles)
        double score = calculerScoreEquipements(l);
        
        System.out.println("[RECOMMENDATION] Logement: " + l.getNom() + 
            " - Score équipements: " + score);

        if (prefs == null) {
            return Math.min(5.0, Math.max(0.0, score));
        }

        // ✅ Bonus pour équipements souhaités
        Set<String> equipementsLogement = extractEquipements(l);
        Set<String> equipementsSouhaites = parseEquipementsSouhaites(prefs.getEquipementsSouhaites());
        
        double equipementsBonus = 0;
        int matchCount = 0;
        for (String equipement : equipementsSouhaites) {
            if (equipementsLogement.stream()
                    .anyMatch(e -> e.toLowerCase().contains(equipement.toLowerCase()) || 
                                   equipement.toLowerCase().contains(e.toLowerCase()))) {
                matchCount++;
                equipementsBonus += 0.5;
            }
        }
        
        score += equipementsBonus;
        System.out.println("[RECOMMENDATION]   - Équipements souhaités trouvés: " + matchCount + 
            "/" + equipementsSouhaites.size() + " (+bonus: " + equipementsBonus + ")");

        // 💰 Budget : favoriser logements dans le budget
        if (prefs.getBudgetMax() != null && l.getPrixNuit() != null) {
            double prix = l.getPrixNuit().doubleValue();
            if (prix <= prefs.getBudgetMax()) {
                score += 0.8;
                // bonus si bien en dessous du budget
                if (prix <= prefs.getBudgetMax() * 0.7) {
                    score += 0.4;
                }
            } else {
                score -= 0.5;
            }
        }

        // 🏙️ Ville préférée
        if (prefs.getVillePreferee() != null && l.getVille() != null) {
            if (l.getVille().toLowerCase().contains(prefs.getVillePreferee().toLowerCase())) {
                score += 0.8;
            }
        }

        // 🏠 Type de séjour / catégorie
        if (prefs.getTypeSejour() != null && l.getCategorie() != null) {
            String cat = l.getCategorie().getNomCategorie();
            if (cat != null && cat.toLowerCase().contains(prefs.getTypeSejour().toLowerCase())) {
                score += 0.6;
            }
        }

        // 👥 Capacité minimale
        if (prefs.getCapaciteMin() != null) {
            if (l.getCapacite() >= prefs.getCapaciteMin()) {
                score += 0.4;
            } else {
                score -= 0.3;
            }
        }

        // 🎨 Ambiance dans la description
        if (prefs.getAmbiance() != null && l.getDescription() != null) {
            if (l.getDescription().toLowerCase().contains(prefs.getAmbiance().toLowerCase())) {
                score += 0.4;
            }
        }

        // ⭐ Limiter entre 0 et 5 étoiles
        return Math.min(5.0, Math.max(0.0, score));
    }

    /**
     * 🌟 Calculer le score de base en fonction du nombre d'équipements
     * - 0 équipements: 1 étoile
     * - 1-3 équipements: 2 étoiles
     * - 4-7 équipements: 3 étoiles
     * - 8-12 équipements: 4 étoiles
     * - 13+ équipements: 5 étoiles
     */
    private double calculerScoreEquipements(Logement l) {
        Set<String> equipements = extractEquipements(l);
        int count = equipements.size();
        
        if (count == 0) return 1.0;
        if (count <= 3) return 2.0;
        if (count <= 7) return 3.0;
        if (count <= 12) return 4.0;
        return 5.0;
    }

    /**
     * 📝 Extraire les équipements de la description du logement
     */
    private Set<String> extractEquipements(Logement l) {
        Set<String> found = new HashSet<>();
        
        if (l.getDescription() == null) {
            return found;
        }
        
        String desc = l.getDescription().toLowerCase();
        
        for (String equipement : EQUIPEMENTS_PREMIUM) {
            if (desc.contains(equipement.toLowerCase())) {
                found.add(equipement);
            }
        }
        
        // Parser aussi les équipements JSON si présents
        try {
            if (l.getDescription().contains("\"equipements\"")) {
                ObjectMapper mapper = new ObjectMapper();
                // Utiliser une regex simple pour extraire le JSON
                String jsonPattern = "\\{[^}]*\"equipements\"[^}]*\\}";
                if (l.getDescription().matches(".*" + jsonPattern + ".*")) {
                    // Extraction possible d'équipements structurés
                }
            }
        } catch (Exception e) {
            // Ignorer les erreurs de parsing
        }
        
        return found;
    }

    /**
     * 🔍 Parser les équipements souhaités de l'utilisateur
     */
    private Set<String> parseEquipementsSouhaites(String equipementsSouhaites) {
        Set<String> result = new HashSet<>();
        
        if (equipementsSouhaites == null || equipementsSouhaites.isEmpty()) {
            return result;
        }
        
        // Supposer format séparé par virgules ou JSON
        try {
            if (equipementsSouhaites.startsWith("[")) {
                // Format JSON array
                ObjectMapper mapper = new ObjectMapper();
                String[] items = mapper.readValue(equipementsSouhaites, String[].class);
                for (String item : items) {
                    if (item != null && !item.isEmpty()) {
                        result.add(item.toLowerCase());
                    }
                }
            } else {
                // Format texte séparé par virgules
                String[] parts = equipementsSouhaites.split(",");
                for (String part : parts) {
                    String trimmed = part.trim().toLowerCase();
                    if (!trimmed.isEmpty()) {
                        result.add(trimmed);
                    }
                }
            }
        } catch (Exception e) {
            // Fallback: split simple
            String[] parts = equipementsSouhaites.split(",");
            for (String part : parts) {
                String trimmed = part.trim().toLowerCase();
                if (!trimmed.isEmpty()) {
                    result.add(trimmed);
                }
            }
        }
        
        return result;
    }

    private record ScoredLogement(Logement logement, double score) {}
}

