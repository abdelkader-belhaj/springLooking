package tn.hypercloud.controller.transport;

import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
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
import tn.hypercloud.entity.transport.enums.VehiculeStatut;
import tn.hypercloud.entity.user.User;
import tn.hypercloud.repository.transport.VehiculeRepository;
import tn.hypercloud.service.transport.IDistanceService;
import tn.hypercloud.service.transport.IDemandeCoursService;
import tn.hypercloud.service.transport.ITransportationBookingService;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/hypercloud/demandes-courses")
@AllArgsConstructor
public class DemandeCoursController {

    private final ITransportationBookingService transportationBookingService;
    private final IDemandeCoursService demandeCoursService;   // pour les opérations de base
    private final IDistanceService distanceService;
    private final VehiculeRepository vehiculeRepository;
    private final SimpMessagingTemplate messagingTemplate;

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

                Optional<Vehicule> vehiculeOpt = vehiculeRepository
                    .findByTypeVehiculeAndStatut(typeVehicule, VehiculeStatut.ACTIVE)
                    .stream()
                    .filter(v -> v.getPrixKm() != null && v.getPrixMinute() != null)
                    .findFirst();

                if (vehiculeOpt.isEmpty()) {
                vehiculeOpt = vehiculeRepository
                    .findByTypeVehicule(typeVehicule)
                    .stream()
                    .filter(v -> v.getPrixKm() != null && v.getPrixMinute() != null)
                    .findFirst();
                }

            Localisation depart = new Localisation();
            depart.setLatitude(request.getDepartLatitude());
            depart.setLongitude(request.getDepartLongitude());

            Localisation arrivee = new Localisation();
            arrivee.setLatitude(request.getArriveeLatitude());
            arrivee.setLongitude(request.getArriveeLongitude());

            IDistanceService.RouteInfo routeInfo = distanceService.calculateRoute(depart, arrivee);

            BigDecimal prixKm = vehiculeOpt.map(Vehicule::getPrixKm).orElseGet(() -> switch (typeVehicule) {
                case ECONOMY -> new BigDecimal("2.50");
                case PREMIUM -> new BigDecimal("3.80");
                case VAN -> new BigDecimal("4.50");
            });

            BigDecimal prixMinute = vehiculeOpt.map(Vehicule::getPrixMinute).orElseGet(() -> switch (typeVehicule) {
                case ECONOMY -> new BigDecimal("0.40");
                case PREMIUM -> new BigDecimal("0.60");
                case VAN -> new BigDecimal("0.70");
            });

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

    @PutMapping("/{id}/confirmer-client")
    public DemandeCourse confirmAcceptedByClient(@PathVariable Long id) {
        DemandeCourse demande = demandeCoursService.confirmAcceptedByClient(id);

        messagingTemplate.convertAndSend(
            "/topic/demande/" + id,
            Map.of(
                "idDemande", id,
                "statut", demande.getStatut() != null ? demande.getStatut().name() : null,
                "approbationClientRequise", demande.getApprobationClientRequise()
            )
        );

        if (demande.getCourse() != null && demande.getCourse().getIdCourse() != null) {
            messagingTemplate.convertAndSend(
                "/topic/course/" + demande.getCourse().getIdCourse() + "/status",
                Map.of(
                    "courseId", demande.getCourse().getIdCourse(),
                    "statut", demande.getCourse().getStatut() != null ? demande.getCourse().getStatut().name() : null,
                    "clientConfirmed", Boolean.FALSE.equals(demande.getApprobationClientRequise())
                )
            );
        }

        return demande;
    }

    @PutMapping("/{id}/confirmer-client/annuler")
    public DemandeCourse cancelClientConfirmation(@PathVariable Long id) {
        DemandeCourse demande = demandeCoursService.cancelClientConfirmation(id);

        messagingTemplate.convertAndSend(
                "/topic/demande/" + id,
                Map.of(
                        "idDemande", id,
                        "statut", demande.getStatut() != null ? demande.getStatut().name() : null,
                        "approbationClientRequise", demande.getApprobationClientRequise(),
                        "confirmationClientStatut", demande.getConfirmationClientStatut() != null
                                ? demande.getConfirmationClientStatut().name()
                                : null
                )
        );

        return demande;
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
        DemandeCourse demande = demandeCoursService.updateStatut(id, status);

        if (demande != null) {
            messagingTemplate.convertAndSend(
                "/topic/demande/" + id,
                Map.of(
                    "idDemande", id,
                    "statut", demande.getStatut().name(),
                    "approbationClientRequise", Boolean.TRUE.equals(demande.getApprobationClientRequise()),
                    "confirmationClientStatut", demande.getConfirmationClientStatut() != null
                        ? demande.getConfirmationClientStatut().name()
                        : "PENDING"
                )
            );

            if (status == DemandeStatus.CANCELLED
                && demande.getCourse() != null
                && demande.getCourse().getIdCourse() != null) {
            messagingTemplate.convertAndSend(
                "/topic/course/" + demande.getCourse().getIdCourse() + "/status",
                Map.of(
                    "courseId", demande.getCourse().getIdCourse(),
                    "statut", "CANCELLED",
                    "demandCancelled", true,
                    "demandeId", id
                )
            );
            }
        }

        return demande;
    }

    // (Optionnel) Suppression – à utiliser avec prudence
    @DeleteMapping("/{id}")
    public void deleteDemandeCourse(@PathVariable Long id) {
        demandeCoursService.deleteDemandeCourse(id);
    }
}
