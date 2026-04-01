package tn.hypercloud.controller.transport;

import tn.hypercloud.entity.transport.ReservationLocation;
import tn.hypercloud.service.transport.IReservationLocationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/hypercloud/reservations-location")
@RequiredArgsConstructor
public class ReservationLocationController {

    private final IReservationLocationService reservationService;

    @PostMapping
    public ReservationLocation createReservation(@RequestBody ReservationLocation reservation) {
        return reservationService.createReservation(reservation);
    }

    @PutMapping("/{id}")
    public ReservationLocation updateReservation(@PathVariable Long id, @RequestBody ReservationLocation reservation) {
        reservation.setIdReservation(id);
        return reservationService.updateReservation(reservation);
    }

    @DeleteMapping("/{id}")
    public void deleteReservation(@PathVariable Long id) {
        reservationService.deleteReservation(id);
    }

    @GetMapping("/{id}")
    public ReservationLocation getById(@PathVariable Long id) {
        return reservationService.getById(id);
    }

    @GetMapping
    public List<ReservationLocation> getAllReservations() {
        return reservationService.getAllReservations();
    }

    @GetMapping("/client/{clientId}")
    public List<ReservationLocation> getByClient(@PathVariable Long clientId) {
        return reservationService.getReservationsByClient(clientId);
    }

    @PutMapping("/{id}/confirmer")
    public ReservationLocation confirm(@PathVariable Long id) {
        return reservationService.confirmReservation(id);
    }

    @PutMapping("/{id}/annuler")
    public ReservationLocation cancel(@PathVariable Long id) {
        return reservationService.cancelReservation(id);
    }
}