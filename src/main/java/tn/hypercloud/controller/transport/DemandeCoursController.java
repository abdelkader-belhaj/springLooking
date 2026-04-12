package tn.hypercloud.controller.transport;

import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.hypercloud.dto.transport.DemandePreauthRequestDto;
import tn.hypercloud.dto.transport.DemandePreauthResponseDto;
import tn.hypercloud.dto.transport.EstimatePriceRequestDto;
import tn.hypercloud.dto.transport.EstimatePriceResponseDto;
import tn.hypercloud.entity.transport.DemandeCourse;
import tn.hypercloud.entity.transport.Localisation;
import tn.hypercloud.entity.transport.Vehicule;
import tn.hypercloud.entity.transport.enums.DemandeStatus;
import tn.hypercloud.entity.transport.enums.TypeVehicule;
import tn.hypercloud.entity.user.User;
import tn.hypercloud.repository.transport.VehiculeRepository;
import tn.hypercloud.service.transport.IDistanceService;
import tn.hypercloud.service.transport.IDemandeCoursService;
import tn.hypercloud.service.transport.ITransportationBookingService;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@RestController
@RequestMapping("/hypercloud/demandes-courses")
@AllArgsConstructor
public class DemandeCoursController {

    private final ITransportationBookingService transportationBookingService;
    private final IDemandeCoursService demandeCoursService;   // pour les opérations de base
    private final IDistanceService distanceService;
    private final VehiculeRepository vehiculeRepository;

    private static final BigDecimal BASE_FEE = new BigDecimal("5.00");

    // =============================================
    // POINT 3 PDF : Création d’une demande par le client
    // =============================================
    @PostMapping
    public DemandeCourse createDemandeCourse(@RequestBody DemandeCourse demandeCourse) {
        return transportationBookingService.createBookingRequest(demandeCourse);
    }

    @PostMapping("/{id}/paiement/preautoriser")
    public DemandePreauthResponseDto preAuthorizePayment(
            @PathVariable Long id,
            @RequestBody(required = false) DemandePreauthRequestDto request
    ) {
        return demandeCoursService.preAuthorizePayment(
                id,
                request != null ? request.getHoldAmount() : null,
                request != null ? request.getPaymentMethodRef() : null
        );
    }

    @PostMapping("/{id}/paiement/preautoriser-penalty")
    public DemandePreauthResponseDto preAuthorizePenalty(
            @PathVariable Long id,
            @RequestBody(required = false) DemandePreauthRequestDto request
    ) {
        DemandeCourse demande = demandeCoursService.getDemandeCoursById(id);
        if (demande == null) {
            throw new IllegalArgumentException("Demande de course non trouvée: " + id);
        }

        BigDecimal estimatedPrice = demande.getPrixEstime() != null
                ? demande.getPrixEstime()
                : BigDecimal.ZERO;

        BigDecimal penaltyAmount = request != null && request.getHoldAmount() != null
                ? request.getHoldAmount()
                : estimatedPrice.multiply(new BigDecimal("0.20")).setScale(2, RoundingMode.HALF_UP);

        return demandeCoursService.preAuthorizePenalty(
                id,
                penaltyAmount,
                request != null ? request.getPaymentMethodRef() : null
        );
    }

    @PostMapping("/estimate")
    public ResponseEntity<?> estimatePrice(@RequestBody EstimatePriceRequestDto request) {
        try {
            if (request == null
                    || request.getDepartLatitude() == null
                    || request.getDepartLongitude() == null
                    || request.getArriveeLatitude() == null
                    || request.getArriveeLongitude() == null
                    || request.getTypeVehiculeDemande() == null
                    || request.getTypeVehiculeDemande().isBlank()) {
                return ResponseEntity.badRequest().body("Champs requis manquants pour l'estimation");
            }

            TypeVehicule typeVehicule;
            try {
                typeVehicule = TypeVehicule.valueOf(request.getTypeVehiculeDemande().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException ex) {
                return ResponseEntity.badRequest().body("Type véhicule invalide: " + request.getTypeVehiculeDemande());
            }

            Optional<Vehicule> vehiculeOpt = vehiculeRepository.findByTypeVehicule(typeVehicule).stream().findFirst();
            if (vehiculeOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body("Aucun véhicule trouvé pour le type: " + typeVehicule);
            }

            Vehicule vehicule = vehiculeOpt.get();

            Localisation depart = new Localisation();
            depart.setLatitude(request.getDepartLatitude());
            depart.setLongitude(request.getDepartLongitude());

            Localisation arrivee = new Localisation();
            arrivee.setLatitude(request.getArriveeLatitude());
            arrivee.setLongitude(request.getArriveeLongitude());

            IDistanceService.RouteInfo routeInfo = distanceService.calculateRoute(depart, arrivee);

            BigDecimal prixKm = vehicule.getPrixKm() != null ? vehicule.getPrixKm() : BigDecimal.ZERO;
            BigDecimal prixMinute = vehicule.getPrixMinute() != null ? vehicule.getPrixMinute() : BigDecimal.ZERO;

            BigDecimal distanceCost = prixKm.multiply(BigDecimal.valueOf(routeInfo.distanceKm()));
            BigDecimal timeCost = prixMinute.multiply(BigDecimal.valueOf(routeInfo.durationMinutes()));

            BigDecimal prixEstime = BASE_FEE
                    .add(distanceCost)
                    .add(timeCost)
                    .setScale(2, RoundingMode.HALF_UP);

            EstimatePriceResponseDto response = EstimatePriceResponseDto.builder()
                    .prixEstimeCalcule(prixEstime)
                    .distanceKm(routeInfo.distanceKm())
                    .dureeEstimeeMinutes(routeInfo.durationMinutes())
                    .build();

            return ResponseEntity.ok(response);
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erreur interne lors de l'estimation: " + ex.getMessage());
        }
    }

    // =============================================
    // POINT 4 PDF : Lancer le matching (broadcast)
    // =============================================
    @PutMapping("/{id}/matching")
    public DemandeCourse startMatching(@PathVariable Long id) {
        return transportationBookingService.startMatching(id);
    }

    // =============================================
    // Opérations de base (CRUD + recherches)
    // =============================================
    @GetMapping("/{id}")
    public DemandeCourse getDemandeCoursById(@PathVariable Long id) {
        return demandeCoursService.getDemandeCoursById(id);
    }

    @GetMapping
    public List<DemandeCourse> getAllDemandeCourses() {
        return demandeCoursService.getAllDemandeCourses();
    }

    @GetMapping("/client/{clientId}")
    public List<DemandeCourse> getDemandesByClient(@PathVariable Long clientId) {
        User client = new User();           // on charge juste l'ID
        client.setId(clientId);
        return transportationBookingService.getBookingsByClient(client);
    }

    @GetMapping("/statut/{statut}")
    public List<DemandeCourse> getDemandesByStatut(@PathVariable String statut) {
        DemandeStatus status = DemandeStatus.valueOf(statut.toUpperCase());
        return demandeCoursService.getDemandesByStatut(status);
    }

    @PutMapping("/{id}/statut/{statut}")
    public DemandeCourse updateStatut(@PathVariable Long id, @PathVariable String statut) {
        DemandeStatus status = DemandeStatus.valueOf(statut.toUpperCase());
        return demandeCoursService.updateStatut(id, status);
    }

    // (Optionnel) Suppression – à utiliser avec prudence
    @DeleteMapping("/{id}")
    public void deleteDemandeCourse(@PathVariable Long id) {
        demandeCoursService.deleteDemandeCourse(id);
    }
}
