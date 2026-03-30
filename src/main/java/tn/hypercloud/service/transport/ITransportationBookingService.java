package tn.hypercloud.service.transport;

import tn.hypercloud.entity.transport.DemandeCourse;
import tn.hypercloud.entity.user.User;

import java.math.BigDecimal;
import java.util.List;

public interface ITransportationBookingService {

    /**
     * POINT 3 du PDF : Création complète d’une demande de course par le client
     * (localisations + type véhicule + calcul prix estimé)
     */
    DemandeCourse createBookingRequest(DemandeCourse demandeCourse);

    /**
     * POINT 4 du PDF : Déclenche le matching (broadcast aux chauffeurs)
     */
    DemandeCourse startMatching(Long demandeId);

    /**
     * Récupère toutes les demandes d’un client (utile pour le frontend)
     */
    List<DemandeCourse> getBookingsByClient(User client);

    /**
     * Calcul du prix estimé (placeholder pour l’instant – on l’améliorera avec distance réelle)
     */
    BigDecimal calculateEstimatedPrice(DemandeCourse demande);
}