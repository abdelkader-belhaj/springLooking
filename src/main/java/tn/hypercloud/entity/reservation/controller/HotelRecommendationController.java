package tn.hypercloud.entity.reservation.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.hypercloud.entity.reservation.dto.HotelRecommendation;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/hotels")
@CrossOrigin("*")
public class HotelRecommendationController {

    @GetMapping("/recommandations")
    public ResponseEntity<List<HotelRecommendation>> getRecommandations(@RequestParam String destination) {
        List<HotelRecommendation> hotels = new ArrayList<>();
        
        // Mock data logic based on destination
        String dest = destination.trim();
        if (dest.equalsIgnoreCase("Paris")) {
            hotels.add(newHotel("Hôtel Ritz Paris", "550€/nuit", 48.8681, 2.3289));
            hotels.add(newHotel("Hôtel Le Meurice", "480€/nuit", 48.8651, 2.3281));
            hotels.add(newHotel("Shangri-La Paris", "620€/nuit", 48.8635, 2.2938));
        } else if (dest.equalsIgnoreCase("Tunis")) {
            hotels.add(newHotel("The Residence Tunis", "320 DT/nuit", 36.9145, 10.2736));
            hotels.add(newHotel("Four Seasons Resort Tunis", "450 DT/nuit", 36.9201, 10.2801));
            hotels.add(newHotel("Mövenpick Hotel du Lac", "280 DT/nuit", 36.8378, 10.2372));
        } else if (dest.equalsIgnoreCase("Dortmund")) {
            hotels.add(newHotel("Hôtel NH Dortmund", "115€/nuit", 51.5175, 7.4611));
            hotels.add(newHotel("Radisson Blu Hotel Dortmund", "145€/nuit", 51.4965, 7.4589));
            hotels.add(newHotel("Mercure Hotel Dortmund City", "95€/nuit", 51.5142, 7.4595));
        } else {
            // Default generic mock data (3 hotels)
            hotels.add(newHotel("Grand Hôtel " + dest, "120€/nuit", 48.8566, 2.3522));
            hotels.add(newHotel("Hôtel de la Paix - " + dest, "85€/nuit", 48.8580, 2.3510));
            hotels.add(newHotel("Résidence Centrale " + dest, "65€/nuit", 48.8550, 2.3535));
        }

        return ResponseEntity.ok(hotels);
    }

    private HotelRecommendation newHotel(String nom, String prix, double lat, double lon) {
        return HotelRecommendation.builder()
                .nom(nom)
                .prixApprox(prix)
                .latitude(lat)
                .longitude(lon)
                .osmLink("https://www.openstreetmap.org/?mlat=" + lat + "&mlon=" + lon + "#map=17/" + lat + "/" + lon)
                .build();
    }
}
