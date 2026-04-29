package tn.hypercloud.controller.accommodation;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import tn.hypercloud.payload.request.LogementRequest;
import tn.hypercloud.payload.response.LogementResponse;
import tn.hypercloud.payload.response.RecommendationResponse;
import tn.hypercloud.service.accommodation.LogementService;
import tn.hypercloud.service.accommodation.RecommendationServicee;
import tn.hypercloud.service.accommodation.GeolocationService;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/logements")
@RequiredArgsConstructor
public class LogementController {

    private final LogementService service;
    private final RecommendationServicee recommendationService;
    private final GeolocationService geolocationService;

    // CREATE LOGEMENT
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','HEBERGEUR')")
    public ResponseEntity<LogementResponse> create(
            @RequestBody LogementRequest req) {

        return ResponseEntity.ok(service.create(req));
    }

    // GET ALL LOGEMENTS — public (SecurityConfig: permitAll)
    @GetMapping
    public ResponseEntity<List<LogementResponse>> getAll() {

        return ResponseEntity.ok(service.getAll());
    }

    // GET ALL LOGEMENTS PUBLIQUES — sans filtre d’utilisateur
    @GetMapping("/public")
    public ResponseEntity<List<LogementResponse>> getAllPublic() {

        return ResponseEntity.ok(service.getAllPublic());
    }

    // GET LOGEMENT BY ID — public (SecurityConfig: permitAll)
    @GetMapping("/{id}")
    public ResponseEntity<LogementResponse> getById(
            @PathVariable Integer id) {

        return ResponseEntity.ok(service.getById(id));
    }

    // GET LOGEMENTS BY CATEGORIE — public (SecurityConfig: permitAll)
    @GetMapping("/categorie/{idCategorie}")
    public ResponseEntity<List<LogementResponse>> getByCategorie(
            @PathVariable Integer idCategorie) {

        return ResponseEntity.ok(service.getByCategorie(idCategorie));
    }

    // GET RECOMMANDATIONS PAR IA (AVEC ETOILES)
    @GetMapping("/recommendations/{userId}")
    public ResponseEntity<List<RecommendationResponse>> getRecommendations(
            @PathVariable Integer userId) {

        return ResponseEntity.ok(recommendationService.getRecommandationsPourUser(userId));
    }

    // UPDATE LOGEMENT
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','HEBERGEUR')")
    public ResponseEntity<LogementResponse> update(
            @PathVariable Integer id,
            @RequestBody LogementRequest req) {

        return ResponseEntity.ok(service.update(id, req));
    }

    // REVERSE GEOCODING — Convertit lat/lon → ville/adresse
    // Public pour que le frontend puisse l'appeler depuis n'importe quel pays
    @GetMapping("/reverse-geocode")
    public ResponseEntity<Map<String, String>> reverseGeocode(
            @RequestParam Double latitude,
            @RequestParam Double longitude) {

        Map<String, String> result = geolocationService.reverseGeocode(latitude, longitude);
        return ResponseEntity.ok(result);
    }

    // DELETE LOGEMENT
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','HEBERGEUR')")
    public ResponseEntity<Void> delete(
            @PathVariable Integer id) {

        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}