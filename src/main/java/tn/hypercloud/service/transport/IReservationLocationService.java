package tn.hypercloud.service.transport;

import org.springframework.transaction.annotation.Transactional;
import tn.hypercloud.entity.transport.ReservationLocation;
import tn.hypercloud.entity.transport.enums.PaiementMethode;

import java.util.List;

public interface IReservationLocationService {

    ReservationLocation createReservation(ReservationLocation reservation);
    ReservationLocation updateReservation(ReservationLocation reservation);
    void deleteReservation(Long id);
    ReservationLocation getById(Long id);
    List<ReservationLocation> getAllReservations();
    List<ReservationLocation> getReservationsByClient(Long clientId);

    // Actions sur la réservation
    ReservationLocation confirmReservation(Long id);
    ReservationLocation cancelReservation(Long id);

    @Transactional
    ReservationLocation completeReservation(Long id, PaiementMethode methode);
}