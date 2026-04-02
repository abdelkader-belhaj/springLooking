package tn.hypercloud.controller.transport;

import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.hypercloud.entity.transport.ReservationLocation;
import tn.hypercloud.entity.transport.enums.PaiementMethode;
import tn.hypercloud.service.transport.IReservationLocationService;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/hypercloud/reservations-location")
@RequiredArgsConstructor
public class ReservationLocationController {

    private final IReservationLocationService reservationService;

    // ==================== CRUD ====================
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

    // ==================== WORKFLOW ====================
    @PutMapping("/{id}/confirmer")
    public ReservationLocation confirm(@PathVariable Long id) {
        return reservationService.confirmReservation(id);
    }

    @PutMapping("/{id}/annuler")
    public ReservationLocation cancel(@PathVariable Long id) {
        return reservationService.cancelReservation(id);
    }

    @PostMapping("/{id}/complete")
    public ResponseEntity<ReservationLocation> completeReservation(
            @PathVariable Long id,
            @RequestParam(defaultValue = "CARD") PaiementMethode methode) {
        ReservationLocation reservation = reservationService.completeReservation(id, methode);
        return ResponseEntity.ok(reservation);
    }

    @PostMapping("/{id}/upload-license")
    public ReservationLocation uploadLicense(
            @PathVariable Long id,
            @RequestParam String numeroPermis,
            @RequestParam String licenseImageUrl,
            @RequestParam LocalDateTime expiryDate) {
        return reservationService.uploadLicense(id, numeroPermis, licenseImageUrl, expiryDate);
    }

    @PostMapping("/{id}/approve-license")
    public ReservationLocation approveLicense(
            @PathVariable Long id,
            @RequestParam boolean approved,
            @RequestParam(required = false) String reason) {
        return reservationService.approveLicense(id, approved, reason);
    }

    @PostMapping("/{id}/sign-contract")
    public ReservationLocation signContract(
            @PathVariable Long id,
            @RequestParam String base64Signature,
            @RequestParam String signedBy) {
        return reservationService.signContract(id, base64Signature, signedBy);
    }

    @PostMapping("/{id}/check-in")
    public ResponseEntity<String> checkIn(@PathVariable Long id, @RequestBody List<String> photoUrls) {
        reservationService.checkInVehicle(id, photoUrls);
        return ResponseEntity.ok("Check-in effectué avec " + photoUrls.size() + " photo(s)");
    }

    @PostMapping("/{id}/check-out")
    public ResponseEntity<String> checkOut(@PathVariable Long id, @RequestBody List<String> photoUrls) {
        reservationService.checkOutVehicle(id, photoUrls);
        return ResponseEntity.ok("Check-out effectué avec " + photoUrls.size() + " photo(s)");
    }

    @GetMapping("/availability")
    public boolean checkAvailability(
            @RequestParam Long vehiculeAgenceId,
            @RequestParam LocalDateTime start,
            @RequestParam LocalDateTime end) {
        return reservationService.isVehicleAvailable(vehiculeAgenceId, start, end);
    }

    // ==================== PDF DOWNLOAD ====================
    @GetMapping("/{id}/contract-pdf")
    public ResponseEntity<Resource> downloadContract(@PathVariable Long id) {
        ReservationLocation reservation = reservationService.getById(id);
        if (reservation == null || reservation.getRentalContract() == null) {
            return ResponseEntity.notFound().build();
        }

        String filePath = reservation.getRentalContract().getContractPdfUrl();
        try {
            Path path = Paths.get(filePath);
            Resource resource = new UrlResource(path.toUri());   // ← plus de cast inutile

            if (resource.exists() && resource.isReadable()) {     // ← correction ici
                return ResponseEntity.ok()
                        .contentType(MediaType.APPLICATION_PDF)
                        .header(HttpHeaders.CONTENT_DISPOSITION,
                                "attachment; filename=\"contrat_" + id + ".pdf\"")
                        .body(resource);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}