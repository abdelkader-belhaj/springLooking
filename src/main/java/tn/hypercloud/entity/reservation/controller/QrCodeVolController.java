package tn.hypercloud.entity.reservation.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import tn.hypercloud.entity.reservation.QrCodeVol;
import tn.hypercloud.entity.reservation.ReservationVol;
import tn.hypercloud.entity.reservation.dto.BilletPublicResponse;
import tn.hypercloud.entity.reservation.dto.QrCodeVolResponse;
import tn.hypercloud.entity.reservation.service.QrCodeService;
import tn.hypercloud.repository.reservation.ReservationVolRepository;

@CrossOrigin(origins = {"http://localhost:4200", "http://192.168.1.13:4200"})
@RestController
@RequestMapping("/api/qrcodes")
@RequiredArgsConstructor
public class QrCodeVolController {

    private final QrCodeService qrCodeService;
    private final ReservationVolRepository reservationRepo;

    // ============================================================
    //  CLIENT : RÉCUPÉRER LE QR CODE (authentifié)
    //  GET /api/qrcodes/reservation/{reservationId}
    // ============================================================
    @GetMapping("/reservation/{reservationId}")
    @PreAuthorize("hasRole('CLIENT_TOURISTE')")
    @Transactional
    public ResponseEntity<QrCodeVolResponse> getByReservation(
            @AuthenticationPrincipal UserDetails user,
            @PathVariable Integer reservationId) {

        ReservationVol res = reservationRepo.findById(reservationId)
                .orElseThrow(() -> new RuntimeException("Réservation introuvable"));

        if (!res.getTouriste().getEmail().equals(user.getUsername()))
            throw new RuntimeException("Accès refusé");

        QrCodeVol qr = qrCodeService.genererEtSauvegarder(res);

        QrCodeVolResponse resp = QrCodeVolResponse.builder()
                .id(qr.getId())
                .reservationId(reservationId)
                .reference(res.getReference())
                .contenu(qr.getContenu())
                .imageBase64(qr.getImageBase64())
                .dateGeneration(qr.getDateGeneration())
                .build();

        return ResponseEntity.ok(resp);
    }

    // ============================================================
    //  PUBLIC : PAGE BILLET VIA QR CODE SCANNÉ
    //  GET /api/qrcodes/reference/{reference}
    //  Pas de @PreAuthorize = accessible sans token (téléphone)
    // ============================================================
    @GetMapping("/reference/{reference}")
    @Transactional
    public ResponseEntity<BilletPublicResponse> getByReference(
            @PathVariable String reference) {

        ReservationVol res = reservationRepo.findByReference(reference)
                .orElseThrow(() -> new RuntimeException("Billet introuvable"));

        QrCodeVol qr = qrCodeService.genererEtSauvegarder(res);

        BilletPublicResponse resp = BilletPublicResponse.builder()
                .reference(res.getReference())
                .touristeNom(res.getTouriste().getUsername())
                .depart(res.getVolAller().getDepart())
                .arrivee(res.getVolAller().getArrivee())
                .numeroVol(res.getVolAller().getNumero())
                .dateDepart(res.getVolAller().getDateDepart().toString())
                .heureDepart(res.getVolAller().getHeureDepart().toString())
                .nbPassagers(res.getNbPassagers())
                .typeBillet(res.getTypeBillet().toString().replace("_", " "))
                .prixTotal(res.getPrixTotal().toString())
                .imageBase64(qr.getImageBase64())
                .build();

        return ResponseEntity.ok(resp);
    }
}