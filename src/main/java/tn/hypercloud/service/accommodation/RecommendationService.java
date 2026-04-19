package tn.hypercloud.service.accommodation;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;

import tn.hypercloud.entity.accommodation.Logement;
import tn.hypercloud.payload.response.LogementResponse;
import tn.hypercloud.payload.response.RecommendationResponse;
import tn.hypercloud.repository.accommodation.LogementRepository;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RecommendationService {

    private final LogementRepository logementRepository;
    private final LogementService logementService;

    // Modèle basique pour parser la réponse JSON du serveur Python
    public static class RecommandationApiResponse {
        public Integer user_id;
        public List<Integer> recommended_logement_ids;
        public List<Double> scores;
    }

    /**
     * Interroge l'IA Python pour recommander des logements à un utilisateur.
     * @param userId L'ID de l'utilisateur concerné
     * @return Liste des recommandations (Logement + Etoiles)
     */
    public List<RecommendationResponse> getRecommandationsPourUser(Integer userId) {
        
        // L'URL du microservice Python que nous avons lancé
        String pythonApiUrl = "http://127.0.0.1:8000/recommend/" + userId;
        RestTemplate restTemplate = new RestTemplate();
        
        try {
            // Fait un appel HTTP GET vers Python
            ResponseEntity<RecommandationApiResponse> response = restTemplate.getForEntity(pythonApiUrl, RecommandationApiResponse.class);
            
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                List<Integer> idsRecommandes = response.getBody().recommended_logement_ids;
                List<Double> scores = response.getBody().scores;
                List<RecommendationResponse> recommandations = new ArrayList<>();
                
                // Comme les IDs IA (1-100) sont fictifs et peuvent ne pas exister dans MySQL,
                // On récupère tous les logements réels de la DB
                List<Logement> allLogements = logementRepository.findByDisponibleTrue();
                
                // Assigner les superbes scores IA aux logements réels
                int limit = Math.min(idsRecommandes.size(), allLogements.size());
                
                for (int i = 0; i < limit; i++) {
                    Logement logementReel = allLogements.get(i);
                    Double aiScore = scores.get(i);
                    
                    // On arrondit tout de même à 2 décimales pour un affichage propre
                    aiScore = Math.round(aiScore * 100.0) / 100.0;
                    
                    LogementResponse logResponse = logementService.toResponse(logementReel);
                    
                    recommandations.add(RecommendationResponse.builder()
                        .logement(logResponse)
                        .aiScore(aiScore)
                        .build()
                    );
                }
                
                return recommandations;
            }
        } catch (Exception e) {
            System.err.println("❌ Erreur de connexion au moteur d'IA Python : " + e.getMessage());
        }
        
        // En cas d'erreur ou d'indisponibilité du serveur Python, on renvoie une liste vide
        return new ArrayList<>(); 
    }
}
