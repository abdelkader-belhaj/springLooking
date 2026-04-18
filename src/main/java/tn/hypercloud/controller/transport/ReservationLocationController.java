package tn.hypercloud.controller.transport;

import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.hypercloud.dto.transport.EtatDesLieuxPhotoDto;
import tn.hypercloud.dto.transport.ReservationLocationDto;
import tn.hypercloud.entity.transport.EtatDesLieuxPhoto;
import tn.hypercloud.entity.transport.ReservationLocation;
import tn.hypercloud.entity.transport.enums.PaiementMethode;
import tn.hypercloud.service.transport.IReservationLocationService;
import tn.hypercloud.service.transport.PdfService;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/hypercloud/reservations-location")
@RequiredArgsConstructor
public class ReservationLocationController {

    private final IReservationLocationService reservationService;
    private final PdfService pdfService;

    @PostMapping
    public ResponseEntity<ReservationLocationDto> createReservation(@RequestBody ReservationLocation reservation) {
        ReservationLocation saved = reservationService.createReservation(reservation);
        return ResponseEntity.ok(toDto(saved));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ReservationLocationDto> updateReservation(@PathVariable Long id, @RequestBody ReservationLocation reservation) {
        reservation.setIdReservation(id);
        ReservationLocation updated = reservationService.updateReservation(reservation);
        return ResponseEntity.ok(toDto(updated));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ReservationLocationDto> getById(@PathVariable Long id) {
        ReservationLocation reservation = reservationService.getById(id);
        if (reservation == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(toDto(reservation));
    }

    @DeleteMapping("/{id}")
    public void deleteReservation(@PathVariable Long id) {
        reservationService.deleteReservation(id);
    }



    @GetMapping
    public List<ReservationLocationDto> getAllReservations() {
        return reservationService.getAllReservations().stream().map(this::toDto).toList();
    }

    @GetMapping("/client/{clientId}")
    public List<ReservationLocationDto> getByClient(@PathVariable Long clientId) {
        return reservationService.getReservationsByClient(clientId)
                .stream()
                .map(this::toDto)
                .toList();
    }

    @GetMapping("/agence/{agenceId}")
    public List<ReservationLocationDto> getByAgence(@PathVariable Long agenceId) {
        return reservationService.getReservationsByAgence(agenceId)
                .stream()
                .map(this::toDto)
                .toList();
    }

    @PutMapping("/{id}/confirmer")
    public ResponseEntity<ReservationLocationDto> confirm(@PathVariable Long id) {
        ReservationLocation reservation = reservationService.confirmReservation(id);
        return ResponseEntity.ok(toDto(reservation));
    }

    @PutMapping("/{id}/annuler")
    public ResponseEntity<ReservationLocationDto> cancel(
            @PathVariable Long id,
            @RequestParam(defaultValue = "CLIENT") String cancelledBy,
            @RequestParam(required = false) String reason) {
        ReservationLocation reservation = reservationService.cancelReservation(id, cancelledBy, reason);
        return ResponseEntity.ok(toDto(reservation));
    }

    @PostMapping("/{id}/paiement/avance")
    public ResponseEntity<ReservationLocationDto> payAdvance(
            @PathVariable Long id,
            @RequestParam(defaultValue = "CARD") PaiementMethode methode,
            @RequestParam(required = false) String paymentIntentId) {
        ReservationLocation reservation = reservationService.payAdvance(id, methode, paymentIntentId);
        return ResponseEntity.ok(toDto(reservation));
    }

    @PostMapping("/{id}/complete")
    public ResponseEntity<ReservationLocationDto> completeReservation(
            @PathVariable Long id,
            @RequestParam(defaultValue = "CARD") PaiementMethode methode,
            @RequestParam(required = false) String paymentIntentId) {
        ReservationLocation reservation = reservationService.completeReservation(id, methode, paymentIntentId);
        return ResponseEntity.ok(toDto(reservation));
    }

    @PostMapping("/{id}/upload-license")
    public ResponseEntity<ReservationLocationDto> uploadLicense(
            @PathVariable Long id,
            @RequestParam String numeroPermis,
            @RequestParam String licenseImageUrl,
            @RequestParam String expiryDate,
            @RequestParam String prenom,
            @RequestParam String nom,
            @RequestParam String dateNaiss) {
        LocalDateTime parsedExpiry = expiryDate.length() == 10
                ? java.time.LocalDate.parse(expiryDate).atStartOfDay()
                : java.time.LocalDateTime.parse(expiryDate);
        LocalDateTime parsedDateNaiss = dateNaiss.length() == 10
                ? java.time.LocalDate.parse(dateNaiss).atStartOfDay()
                : java.time.LocalDateTime.parse(dateNaiss);
        ReservationLocation reservation = reservationService.uploadLicense(id, numeroPermis, licenseImageUrl, parsedExpiry, prenom,
                nom,
                parsedDateNaiss);
        return ResponseEntity.ok(toDto(reservation));
    }

    @PostMapping("/{id}/approve-license")
    public ResponseEntity<ReservationLocationDto> approveLicense(
            @PathVariable Long id,
            @RequestParam boolean approved,
            @RequestParam(required = false) String reason) {
        ReservationLocation reservation = reservationService.approveLicense(id, approved, reason);
        return ResponseEntity.ok(toDto(reservation));
    }

    @PostMapping("/{id}/sign-contract")
    public ResponseEntity<ReservationLocationDto> signContract(
            @PathVariable Long id,
            @RequestParam String base64Signature,
            @RequestParam String signedBy) {
        ReservationLocation reservation = reservationService.signContract(id, base64Signature, signedBy);
        return ResponseEntity.ok(toDto(reservation));
    }

    @PostMapping("/{id}/check-in")
    public ResponseEntity<String> checkIn(@PathVariable Long id, @RequestBody List<String> photoUrls) {
        reservationService.checkInVehicle(id, photoUrls);
        return ResponseEntity.ok("Check-in effectue avec " + photoUrls.size() + " photo(s)");
    }

    @PostMapping("/{id}/check-out")
    public ResponseEntity<String> checkOut(@PathVariable Long id, @RequestBody List<String> photoUrls) {
        reservationService.checkOutVehicle(id, photoUrls);
        return ResponseEntity.ok("Check-out effectue avec " + photoUrls.size() + " photo(s)");
    }

    @GetMapping("/availability")
    public boolean checkAvailability(
            @RequestParam Long vehiculeAgenceId,
            @RequestParam LocalDateTime start,
            @RequestParam LocalDateTime end) {
        return reservationService.isVehicleAvailable(vehiculeAgenceId, start, end);
    }

    @GetMapping("/{id}/contract-pdf")
    public ResponseEntity<Resource> downloadContract(@PathVariable Long id) {
        ReservationLocation reservation = reservationService.getById(id);
        if (reservation == null) return ResponseEntity.notFound().build();

        try {
            if (reservation.getRentalContract() == null || reservation.getRentalContract().getContractPdfUrl() == null) {
                String generatedPath = pdfService.generateContractPdf(reservation);
                reservation.getRentalContract().setContractPdfUrl(generatedPath);
            }

            String filePath = reservation.getRentalContract().getContractPdfUrl();
            Path path = Paths.get(filePath);
            if (!path.isAbsolute()) {
                path = Paths.get(System.getProperty("user.dir")).resolve(path).normalize();
            }

            Resource resource = new UrlResource(path.toUri());
            if (!resource.exists() || !resource.isReadable()) {
                return ResponseEntity.notFound().build();
            }

            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_PDF)
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"contrat_" + id + ".pdf\"")
                    .body(resource);

        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/{id}/final-invoice-pdf")
    public ResponseEntity<Resource> downloadFinalInvoice(@PathVariable Long id) {
        ReservationLocation reservation = reservationService.getById(id);
        if (reservation == null) return ResponseEntity.notFound().build();

        String phase = String.valueOf(reservation.getPaymentPhase() == null ? "" : reservation.getPaymentPhase()).toUpperCase();
        boolean cancellationWithRefund = "CANCELLED_REFUNDED_TOTAL".equals(phase)
            || "CANCELLED_BY_AGENCY_REFUND_TOTAL".equals(phase)
            || "CANCELLED_DEPOSIT_REFUNDED_ADVANCE_LOST".equals(phase);
        String downloadFileName = cancellationWithRefund
            ? "facture_annulation_remboursement_" + id + ".pdf"
            : "facture_location_" + id + ".pdf";

        try {
            String filePath = pdfService.generateFinalInvoicePdf(reservation);
            Path path = Paths.get(filePath);
            if (!path.isAbsolute()) {
                path = Paths.get(System.getProperty("user.dir")).resolve(path).normalize();
            }

            Resource resource = new UrlResource(path.toUri());
            if (!resource.exists() || !resource.isReadable()) {
                return ResponseEntity.notFound().build();
            }

            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_PDF)
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + downloadFileName + "\"")
                    .body(resource);

        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    private ReservationLocationDto toDto(ReservationLocation r) {
        Long clientId = (r.getClient() != null) ? r.getClient().getId() : r.getClientId();
        String clientUsername = (r.getClient() != null) ? r.getClient().getUsername() : null;

        Long vehiculeId = (r.getVehiculeAgence() != null) ? r.getVehiculeAgence().getIdVehiculeAgence() : r.getVehiculeAgenceId();
        String vehiculeMarque = (r.getVehiculeAgence() != null) ? r.getVehiculeAgence().getMarque() : null;
        String vehiculeModele = (r.getVehiculeAgence() != null) ? r.getVehiculeAgence().getModele() : null;
        String vehiculePlaque = (r.getVehiculeAgence() != null) ? r.getVehiculeAgence().getNumeroPlaque() : null;

        Long agenceId = null;
        String agenceNom = null;
        String agenceAdresse = null;
        String agenceTelephone = null;

        if (r.getAgenceLocation() != null) {
            agenceId = r.getAgenceLocation().getIdAgence();
            agenceNom = r.getAgenceLocation().getNomAgence();
            agenceAdresse = r.getAgenceLocation().getAdresse();
            agenceTelephone = r.getAgenceLocation().getTelephone();
        } else if (r.getVehiculeAgence() != null && r.getVehiculeAgence().getAgence() != null) {
            agenceId = r.getVehiculeAgence().getAgence().getIdAgence();
            agenceNom = r.getVehiculeAgence().getAgence().getNomAgence();
            agenceAdresse = r.getVehiculeAgence().getAgence().getAdresse();
            agenceTelephone = r.getVehiculeAgence().getAgence().getTelephone();
        }

        String contractPdfUrl = (r.getRentalContract() != null) ? r.getRentalContract().getContractPdfUrl() : null;
        List<EtatDesLieuxPhotoDto> photos = r.getEtatDesLieuxPhotos() == null
                ? List.of()
                : r.getEtatDesLieuxPhotos().stream()
                .map(this::toEtatDesLieuxPhotoDto)
                .toList();
        return new ReservationLocationDto(
                r.getIdReservation(),

                clientId,
                clientUsername,

                vehiculeId,
                vehiculeMarque,
                vehiculeModele,
                vehiculePlaque,

                agenceId,
                agenceNom,
                agenceAdresse,
                agenceTelephone,

                r.getDateDebut(),
                r.getDateFin(),

                r.getPrixTotal(),
                r.getAdvanceAmount(),
                r.getDepositAmount(),
                r.getDepositStatus(),

                r.getPaymentPhase(),
                r.getAdvanceStatus(),
                r.getPaymentIntentId(),

                r.getStatut(),
                r.getPrenom(),
                r.getNom(),
                r.getDateNaiss(),
                r.getNumeroPermis(),
                r.getLicenseStatus(),
                r.getLicenseExpiryDate(),
                r.getLicenseImageUrl(),
                r.getNote(),
                r.getLicenseRejectionReason(),
                r.getRejectionReason(),
                contractPdfUrl,
                photos
        );
    }
    private EtatDesLieuxPhotoDto toEtatDesLieuxPhotoDto(EtatDesLieuxPhoto photo) {
        return new EtatDesLieuxPhotoDto(
                photo.getId(),
                photo.getPhotoUrl(),
                photo.getType(),
                photo.getDateUpload()
        );
    }
    @GetMapping("/{id}/license-image")
    public ResponseEntity<Resource> downloadLicenseImage(@PathVariable Long id) {
        ReservationLocation reservation = reservationService.getById(id);
        if (reservation == null || reservation.getLicenseImageUrl() == null || reservation.getLicenseImageUrl().isBlank()) {
            return ResponseEntity.notFound().build();
        }

        try {
            Path path = Paths.get(reservation.getLicenseImageUrl());
            if (!path.isAbsolute()) {
                path = Paths.get(System.getProperty("user.dir")).resolve(path).normalize();
            }

            Resource resource = new UrlResource(path.toUri());
            if (!resource.exists() || !resource.isReadable()) {
                return ResponseEntity.notFound().build();
            }

            String mime = Files.probeContentType(path); // ex: image/png
            MediaType mediaType = (mime != null)
                    ? MediaType.parseMediaType(mime)
                    : MediaType.APPLICATION_OCTET_STREAM;

            return ResponseEntity.ok()
                    .contentType(mediaType)
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + path.getFileName() + "\"")
                    .body(resource);

        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    @PostMapping("/{id}/hold-deposit")
    public ResponseEntity<ReservationLocationDto> holdDeposit(
            @PathVariable Long id,
            @RequestParam(defaultValue = "PHYSICAL") String mode) {
        ReservationLocation reservation = reservationService.holdDeposit(id, mode);
        return ResponseEntity.ok(toDto(reservation));
    }

    @PostMapping("/{id}/refund-deposit")
    public ResponseEntity<ReservationLocationDto> refundDeposit(
            @PathVariable Long id,
            @RequestParam(defaultValue = "CARD") PaiementMethode methode,
            @RequestParam(required = false) String paymentIntentId) {
        ReservationLocation reservation = reservationService.refundDeposit(id, methode, paymentIntentId);
        return ResponseEntity.ok(toDto(reservation));
    }
}