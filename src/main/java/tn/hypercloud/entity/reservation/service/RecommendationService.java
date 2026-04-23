package tn.hypercloud.entity.reservation.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tn.hypercloud.entity.reservation.ReservationVol;
import tn.hypercloud.entity.reservation.Vol;
import tn.hypercloud.repository.reservation.ReservationVolRepository;
import tn.hypercloud.repository.reservation.VolRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RecommendationService {

    private final ReservationVolRepository reservationVolRepository;
    private final VolRepository volRepository;

    public List<Vol> getRecommendedVols(Long userId) {
        List<ReservationVol> userReservations = reservationVolRepository.findByTouristeId(userId);
        
        if (userReservations.isEmpty()) {
            return Collections.emptyList();
        }

        // Créer le profil utilisateur (vecteur de caractéristiques)
        UserProfile userProfile = createUserProfile(userReservations);
        
        // Récupérer tous les vols disponibles
        List<Vol> allAvailableVols = volRepository.findAll()
                .stream()
                .filter(vol -> vol.getPlaces() > 0)
                .collect(Collectors.toList());

        // Appliquer KNN : trouver les vols les plus similaires
        return applyKNN(userProfile, allAvailableVols, 5); // k=5 voisins
    }

    // ====================
    // KNN ALGORITHM
    // ====================

    private UserProfile createUserProfile(List<ReservationVol> reservations) {
        Map<String, Integer> destinationCounts = new HashMap<>();
        BigDecimal totalPrice = BigDecimal.ZERO;
        Set<String> allDepartures = new HashSet<>();
        
        for (ReservationVol reservation : reservations) {
            String destination = reservation.getVolAller().getArrivee();
            destinationCounts.put(destination, destinationCounts.getOrDefault(destination, 0) + 1);
            allDepartures.add(reservation.getVolAller().getDepart());
            totalPrice = totalPrice.add(reservation.getPrixTotal());
            
            if (reservation.getVolRetour() != null) {
                String returnDestination = reservation.getVolRetour().getArrivee();
                destinationCounts.put(returnDestination, destinationCounts.getOrDefault(returnDestination, 0) + 1);
                allDepartures.add(reservation.getVolRetour().getDepart());
            }
        }
        
        BigDecimal avgPrice = reservations.isEmpty() ? BigDecimal.ZERO : 
            totalPrice.divide(BigDecimal.valueOf(reservations.size()), 2, RoundingMode.HALF_UP);
        
        return new UserProfile(destinationCounts, avgPrice, allDepartures);
    }

    private List<Vol> applyKNN(UserProfile userProfile, List<Vol> candidateVols, int k) {
        // Calculer la distance pour chaque vol
        List<VolDistance> volDistances = candidateVols.stream()
            .map(vol -> new VolDistance(vol, calculateDistance(userProfile, vol)))
            .sorted(Comparator.comparingDouble(VolDistance::getDistance))
            .collect(Collectors.toList());
        
        // Retourner les k vols les plus proches
        return volDistances.stream()
            .limit(k)
            .map(VolDistance::getVol)
            .collect(Collectors.toList());
    }

    private double calculateDistance(UserProfile userProfile, Vol vol) {
        // Distance euclidienne simplifiée
        double distance = 0.0;
        
        // 1. Distance par destination (plus important)
        Integer destCount = userProfile.getDestinationCounts().get(vol.getArrivee());
        if (destCount == null) {
            distance += 2.0; // pénalité si destination jamais visitée
        } else {
            distance += 1.0 / (destCount + 1); // plus la destination est visitée, plus la distance est petite
        }
        
        // 2. Distance par prix
        if (userProfile.getAvgPrice().compareTo(BigDecimal.ZERO) > 0) {
            double priceDiff = Math.abs(vol.getPrix().doubleValue() - userProfile.getAvgPrice().doubleValue());
            distance += priceDiff / userProfile.getAvgPrice().doubleValue();
        }
        
        // 3. Distance par départ
        if (!userProfile.getPreferredDepartures().contains(vol.getDepart())) {
            distance += 0.5; // petite pénalité si départ jamais utilisé
        }
        
        return distance;
    }

    // Classes internes pour KNN
    private static class UserProfile {
        private final Map<String, Integer> destinationCounts;
        private final BigDecimal avgPrice;
        private final Set<String> preferredDepartures;
        
        public UserProfile(Map<String, Integer> destinationCounts, BigDecimal avgPrice, Set<String> preferredDepartures) {
            this.destinationCounts = destinationCounts;
            this.avgPrice = avgPrice;
            this.preferredDepartures = preferredDepartures;
        }
        
        public Map<String, Integer> getDestinationCounts() { return destinationCounts; }
        public BigDecimal getAvgPrice() { return avgPrice; }
        public Set<String> getPreferredDepartures() { return preferredDepartures; }
    }
    
    private static class VolDistance {
        private final Vol vol;
        private final double distance;
        
        public VolDistance(Vol vol, double distance) {
            this.vol = vol;
            this.distance = distance;
        }
        
        public Vol getVol() { return vol; }
        public double getDistance() { return distance; }
    }

    // ====================
    // ANCIENNE MÉTHODE (conservée pour référence)
    // ====================

    private Map<String, Integer> calculateDestinationPreferences(List<ReservationVol> reservations) {
        Map<String, Integer> preferences = new HashMap<>();
        
        for (ReservationVol reservation : reservations) {
            String destination = reservation.getVolAller().getArrivee();
            preferences.put(destination, preferences.getOrDefault(destination, 0) + 1);
            
            if (reservation.getVolRetour() != null) {
                String returnDestination = reservation.getVolRetour().getArrivee();
                preferences.put(returnDestination, preferences.getOrDefault(returnDestination, 0) + 1);
            }
        }
        
        return preferences;
    }
}
