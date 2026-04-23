package tn.hypercloud.service.transport;

import tn.hypercloud.entity.transport.Localisation;

public interface IDistanceService {

    /**
     * Calcule la distance et la durée réelle entre deux localisations via OSRM
     * @return un objet contenant distance en km + durée en minutes
     */
    RouteInfo calculateRoute(Localisation origin, Localisation destination);

    record RouteInfo(
            double distanceKm,      // distance en kilomètres
            double durationMinutes  // durée estimée en minutes
    ) {}
}