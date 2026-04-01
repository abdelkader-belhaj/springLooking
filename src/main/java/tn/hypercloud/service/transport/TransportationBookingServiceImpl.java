package tn.hypercloud.service.transport;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.hypercloud.entity.transport.DemandeCourse;
import tn.hypercloud.entity.transport.enums.DemandeStatus;
import tn.hypercloud.entity.transport.enums.TypeVehicule;
import tn.hypercloud.entity.user.User;
import tn.hypercloud.repository.transport.DemandeCoursRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
@AllArgsConstructor
public class TransportationBookingServiceImpl implements ITransportationBookingService {

    private final IDemandeCoursService demandeCoursService;
    private final IMatchingService matchingService;
    private final DemandeCoursRepository demandeCoursRepository;
    private final IDistanceService distanceService;

    // ─────────────────────────────────────────────────────────────
    // 1. CREATE BOOKING REQUEST (client side)
    // ─────────────────────────────────────────────────────────────
    @Override
    @Transactional
    public DemandeCourse createBookingRequest(DemandeCourse demande) {

        // 1. Save first (DemandeCoursService will hydrate full Localisation entities)
        DemandeCourse savedDemande = demandeCoursService.addDemandeCourse(demande);

        // 2. Calculate real estimated price using live route info
        BigDecimal prixEstime = calculateEstimatedPrice(savedDemande);
        savedDemande.setPrixEstime(prixEstime);

        return demandeCoursRepository.save(savedDemande);
    }


    @Override
    public BigDecimal calculateEstimatedPrice(DemandeCourse demande) {
        if (demande.getLocalisationDepart() == null || demande.getLocalisationArrivee() == null) {
            throw new IllegalArgumentException("Les localisations de départ et d'arrivée sont obligatoires pour le calcul du prix");
        }

        // Get real distance + duration via your distance service
        IDistanceService.RouteInfo route = distanceService.calculateRoute(
                demande.getLocalisationDepart(),
                demande.getLocalisationArrivee()
        );

        return calculateEstimatedPriceWithRoute(route, demande.getTypeVehiculeDemande());
    }


    private BigDecimal calculateEstimatedPriceWithRoute(
            IDistanceService.RouteInfo route,
            TypeVehicule typeVehicule) {

        BigDecimal baseFee = new BigDecimal("5.00");

        BigDecimal pricePerKm = switch (typeVehicule) {
            case ECONOMY -> new BigDecimal("2.50");
            case PREMIUM -> new BigDecimal("3.80");
            case VAN     -> new BigDecimal("4.50");
        };

        BigDecimal pricePerMin = switch (typeVehicule) {
            case ECONOMY -> new BigDecimal("0.40");
            case PREMIUM -> new BigDecimal("0.60");
            case VAN     -> new BigDecimal("0.70");
        };

        BigDecimal distanceCost = pricePerKm.multiply(BigDecimal.valueOf(route.distanceKm()));
        BigDecimal timeCost     = pricePerMin.multiply(BigDecimal.valueOf(route.durationMinutes()));

        return baseFee.add(distanceCost).add(timeCost)
                .setScale(2, RoundingMode.HALF_UP);
    }

// 3. START MATCHING
    @Override
    @Transactional
    public DemandeCourse startMatching(Long demandeId) {
        // 1. Récupérer la demande complète
        DemandeCourse demande = demandeCoursService.getDemandeCoursById(demandeId);
        if (demande == null) {
            throw new RuntimeException("Demande de course non trouvée avec l'id: " + demandeId);
        }

        // 2. Passer la demande en MATCHING
        demande = demandeCoursService.updateStatut(demandeId, DemandeStatus.MATCHING);

        // 3. Broadcast aux chauffeurs disponibles → création des Matching PROPOSED
        matchingService.proposeMatchingsToAvailableDrivers(demande);

        return demande;
    }

    // 4. GET BOOKINGS BY CLIENT
    @Override
    public List<DemandeCourse> getBookingsByClient(User client) {
        return demandeCoursRepository.findByClient(client);
    }
}