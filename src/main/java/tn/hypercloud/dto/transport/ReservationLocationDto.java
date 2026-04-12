package tn.hypercloud.dto.transport;

import tn.hypercloud.entity.transport.enums.DepositStatus;
import tn.hypercloud.entity.transport.enums.LicenseStatus;
import tn.hypercloud.entity.transport.enums.ReservationStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record ReservationLocationDto(
        Long idReservation,

        Long clientId,
        String clientUsername,

        Long vehiculeAgenceId,
        String vehiculeMarque,
        String vehiculeModele,
        String vehiculePlaque,

        Long agenceId,
        String agenceNom,
        String agenceAdresse,
        String agenceTelephone,

        LocalDateTime dateDebut,
        LocalDateTime dateFin,

        BigDecimal prixTotal,
        BigDecimal advanceAmount,
        BigDecimal depositAmount,
        DepositStatus depositStatus,

        String paymentPhase,
        String advanceStatus,
        String paymentIntentId,

        ReservationStatus statut,
        String prenom,
        String nom,
        LocalDateTime dateNaiss,
        String numeroPermis,
        LicenseStatus licenseStatus,
        LocalDateTime licenseExpiryDate,
        String licenseImageUrl,
        String note,
        String contractPdfUrl,
        List<EtatDesLieuxPhotoDto> etatDesLieuxPhotos


) {}