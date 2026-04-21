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
        if (destination.equalsIgnoreCase("Paris")) {
            hotels.add(HotelRecommendation.builder()
                    .nom("Hôtel Ritz Paris")
                    .prixApprox("500€/nuit")
                    .latitude(48.8681)
                    .longitude(2.3289)
                    .osmLink("https://www.openstreetmap.org/?mlat=48.8681&mlon=2.3289#map=17/48.8681/2.3289")
                    .build());
            hotels.add(HotelRecommendation.builder()
                    .nom("Hôtel Le Meurice")
                    .prixApprox("450€/nuit")
                    .latitude(48.8651)
                    .longitude(2.3281)
                    .osmLink("https://www.openstreetmap.org/?mlat=48.8651&mlon=2.3281#map=17/48.8651/2.3281")
                    .build());
        } else if (destination.equalsIgnoreCase("Tunis")) {
            hotels.add(HotelRecommendation.builder()
                    .nom("The Residence Tunis")
                    .prixApprox("300 DT/nuit")
                    .latitude(36.9145)
                    .longitude(10.2736)
                    .osmLink("https://www.openstreetmap.org/?mlat=36.9145&mlon=10.2736#map=17/36.9145/10.2736")
                    .build());
            hotels.add(HotelRecommendation.builder()
                    .nom("Four Seasons Resort Tunis")
                    .prixApprox("400 DT/nuit")
                    .latitude(36.9201)
                    .longitude(10.2801)
                    .osmLink("https://www.openstreetmap.org/?mlat=36.9201&mlon=10.2801#map=17/36.9201/10.2801")
                    .build());
        } else {
            // Default generic mock data
            hotels.add(HotelRecommendation.builder()
                    .nom("Hôtel Destination " + destination)
                    .prixApprox("100€/nuit")
                    .latitude(0.0)
                    .longitude(0.0)
                    .osmLink("https://www.openstreetmap.org/")
                    .build());
        }

        return ResponseEntity.ok(hotels);
    }
}
