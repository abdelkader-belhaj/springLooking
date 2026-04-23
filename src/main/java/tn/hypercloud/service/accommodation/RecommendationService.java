package tn.hypercloud.service.accommodation;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import tn.hypercloud.entity.accommodation.Logement;
import tn.hypercloud.entity.accommodation.UserPreferences;
import tn.hypercloud.payload.response.LogementResponse;
import tn.hypercloud.payload.response.RecommendationResponse;
import tn.hypercloud.repository.accommodation.LogementRepository;
import tn.hypercloud.repository.accommodation.UserPreferencesRepository;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RecommendationService {

    private final LogementRepository logementRepository;
    private final LogementService logementService;
    private final UserPreferencesRepository prefsRepository;

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
        double score = 3.0; // score de base

        if (prefs == null) return score;

        // Budget : favoriser logements dans le budget
        if (prefs.getBudgetMax() != null && l.getPrixNuit() != null) {
            double prix = l.getPrixNuit().doubleValue();
            if (prix <= prefs.getBudgetMax()) {
                score += 1.5;
                // bonus si bien en dessous du budget
                if (prix <= prefs.getBudgetMax() * 0.7) score += 0.5;
            } else {
                score -= 1.0;
            }
        }

        // Ville préférée
        if (prefs.getVillePreferee() != null && l.getVille() != null) {
            if (l.getVille().toLowerCase().contains(prefs.getVillePreferee().toLowerCase())) {
                score += 1.5;
            }
        }

        // Type de séjour / catégorie
        if (prefs.getTypeSejour() != null && l.getCategorie() != null) {
            String cat = l.getCategorie().getNomCategorie();
            if (cat != null && cat.toLowerCase().contains(prefs.getTypeSejour().toLowerCase())) {
                score += 1.0;
            }
        }

        // Capacité minimale
        if (prefs.getCapaciteMin() != null) {
            if (l.getCapacite() >= prefs.getCapaciteMin()) {
                score += 0.5;
            } else {
                score -= 0.5;
            }
        }

        // Ambiance dans la description
        if (prefs.getAmbiance() != null && l.getDescription() != null) {
            if (l.getDescription().toLowerCase().contains(prefs.getAmbiance().toLowerCase())) {
                score += 0.5;
            }
        }

        return Math.min(5.0, Math.max(0.0, score));
    }

    private record ScoredLogement(Logement logement, double score) {}
}

