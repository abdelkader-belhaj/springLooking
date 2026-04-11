package tn.hypercloud.service.transport;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.hypercloud.entity.transport.*;
import tn.hypercloud.entity.transport.enums.*;
import tn.hypercloud.entity.user.User;
import tn.hypercloud.repository.transport.*;
import tn.hypercloud.repository.user.UserRepository;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReservationLocationServiceImpl implements IReservationLocationService {

    private final ReservationLocationRepository reservationRepository;
    private final VehiculeAgenceRepository vehiculeAgenceRepository;
    private final UserRepository userRepository;
    private final IPaiementService paiementService;           // comme il existe
    private final PaiementRepository paiementRepository;      // comme il existe
    private final AgenceLocationRepository agenceLocationRepository; // comme il existe
    private final WalletTransactionRepository walletTransactionRepository; // comme il existe
    private final RentalContractRepository contractRepository;
    private final PdfService pdfService;
    private static final Path LICENSE_UPLOAD_DIR = Path.of("uploads", "licenses");
    private static final Path SIGNATURE_UPLOAD_DIR = Path.of("uploads", "signatures");
    private static final Path ETAT_LIEUX_UPLOAD_DIR = Path.of("uploads", "etat-lieux");
    @Override
    @Transactional
    public ReservationLocation createReservation(ReservationLocation reservation) {

        // === RÉSOLUTION DES IDS TRANSIENTS ===
        if (reservation.getClientId() != null) {
            User client = userRepository.findById(reservation.getClientId())
                    .orElseThrow(() -> new RuntimeException("Client non trouvé"));
            reservation.setClient(client);
        }

        if (reservation.getVehiculeAgenceId() != null) {
            VehiculeAgence vehicule = vehiculeAgenceRepository.findById(reservation.getVehiculeAgenceId())
                    .orElseThrow(() -> new RuntimeException("Véhicule d'agence non trouvé"));
            reservation.setVehiculeAgence(vehicule);

            // === CORRECTION PRINCIPALE : on affecte automatiquement l'agence ===
            if (vehicule.getAgence() != null) {
                reservation.setAgenceLocation(vehicule.getAgence());
            }
        }

        // === VÉRIFICATION DISPONIBILITÉ ===
        boolean isBooked = reservationRepository
                .existsByVehiculeAgence_IdVehiculeAgenceAndStatutInAndDateDebutLessThanEqualAndDateFinGreaterThanEqual(
                        reservation.getVehiculeAgence().getIdVehiculeAgence(),
                        List.of(ReservationStatus.CONFIRMED, ReservationStatus.IN_PROGRESS, ReservationStatus.ACTIVE),
                        reservation.getDateDebut(),
                        reservation.getDateFin());

        if (isBooked) {
            throw new IllegalStateException("Véhicule déjà réservé sur cette période");
        }

        // === CALCUL PRIX TOTAL ===
        long days = ChronoUnit.DAYS.between(reservation.getDateDebut(), reservation.getDateFin());
        if (days <= 0) {
            throw new IllegalArgumentException("Date de fin invalide");
        }

        BigDecimal prixTotal = reservation.getVehiculeAgence().getPrixKm()
                .multiply(BigDecimal.valueOf(days));
        reservation.setPrixTotal(prixTotal);
        reservation.setDepositStatus(DepositStatus.PENDING);

        reservation.setStatut(ReservationStatus.KYC_PENDING);
        return reservationRepository.save(reservation);
    }
    @Override
    public ReservationLocation updateReservation(ReservationLocation reservation) {
        return reservationRepository.save(reservation);
    }

    @Override
    public void deleteReservation(Long id) {
        reservationRepository.deleteById(id);
    }

    @Override
    public ReservationLocation getById(Long id) {
        return reservationRepository.findById(id).orElse(null);
    }

    @Override
    public List<ReservationLocation> getAllReservations() {
        return reservationRepository.findAll();
    }

    @Override
    public List<ReservationLocation> getReservationsByClient(Long clientId) {
        return reservationRepository.findByClient_Id(clientId);
    }

    @Override
    @Transactional
    public ReservationLocation confirmReservation(Long id) {
        ReservationLocation res = getById(id);
        if (res == null) throw new RuntimeException("Réservation non trouvée");

        if (res.getDepositStatus() != DepositStatus.HELD) {
            throw new IllegalStateException("La caution doit être HELD avant confirmation");
        }

        if (res.getStatut() != ReservationStatus.DEPOSIT_HELD
                && res.getStatut() != ReservationStatus.CONTRACT_SIGNED) {
            throw new IllegalStateException("Réservation non prête pour confirmation");
        }

        if (res.getStatut() == ReservationStatus.CONFIRMED) {
            return res; // idempotent
        }

        res.setStatut(ReservationStatus.CONFIRMED);
        return reservationRepository.save(res);
    }
    @Override
    @Transactional
    public ReservationLocation cancelReservation(Long id) {
        ReservationLocation res = getById(id);
        if (res == null) throw new RuntimeException("Réservation non trouvée");
        res.setStatut(ReservationStatus.CANCELLED);
        return reservationRepository.save(res);
    }

    @Override
    @Transactional
    public ReservationLocation completeReservation(Long id, PaiementMethode methode) {
        // === CODE PAIEMENT + WALLET TEL QU'IL EXISTE DANS TON PROJET ===
        ReservationLocation reservation = getById(id);
        if (reservation == null) throw new RuntimeException("Réservation non trouvée");

        if (reservation.getStatut() == ReservationStatus.COMPLETED) {
            throw new IllegalStateException("Réservation déjà terminée");
        }

        PaiementTransport paiementTransport = PaiementTransport.builder()
                .reservationLocation(reservation)
                .montantTotal(reservation.getPrixTotal())
                .methode(methode)
                .statut(PaiementStatut.COMPLETED)
                .datePaiement(LocalDateTime.now())
                .build();

        paiementTransport = paiementRepository.save(paiementTransport);

        // === MISE À JOUR SOLDE AGENCE + TRANSACTION WALLET ===
        BigDecimal montantNet = paiementTransport.getMontantNet();
        AgenceLocation agence = reservation.getVehiculeAgence().getAgence();

        // Protection null-safe
        BigDecimal nouveauSolde = (agence.getSolde() != null ? agence.getSolde() : BigDecimal.ZERO)
                .add(montantNet);
        agence.setSolde(nouveauSolde);
        agenceLocationRepository.save(agence);

        walletTransactionRepository.save(WalletTransaction.builder()
                .agence(agence)
                .montant(montantNet)
                .type(TransactionType.CREDIT_RESERVATION)
                .description("Réservation #" + reservation.getIdReservation())
                .paiementTransport(paiementTransport)
                .build());

        reservation.setMontantCommission(paiementTransport.getMontantCommission());
        reservation.setStatut(ReservationStatus.COMPLETED);
        reservation.setDateModification(LocalDateTime.now());

        return reservationRepository.save(reservation);
    }

    @Override
    @Transactional
    public ReservationLocation uploadLicense(Long id, String numeroPermis, String licenseImageUrl, LocalDateTime expiry,String prenom,
                                             String nom,
                                             LocalDateTime dateNaiss) {
        ReservationLocation res = reservationRepository.findById(id).orElseThrow();

        String storedLicensePath = saveBase64ImageIfNeeded(
                licenseImageUrl,
                LICENSE_UPLOAD_DIR,
                "license_" + id
        );

        res.setNumeroPermis(numeroPermis);
        res.setLicenseImageUrl(storedLicensePath); // chemin court en DB
        res.setLicenseExpiryDate(expiry);
        res.setPrenom(prenom != null ? prenom.trim() : null);
        res.setNom(nom != null ? nom.trim() : null);
        res.setDateNaiss(dateNaiss);
        res.setLicenseStatus(LicenseStatus.PENDING);

        return reservationRepository.save(res);
    }
    @Override
    @Transactional
    public ReservationLocation approveLicense(Long id, boolean approved, String reason) {
        ReservationLocation res = reservationRepository.findById(id).orElseThrow();

        if (approved) {
            res.setLicenseStatus(LicenseStatus.APPROVED);

            // Contrat prêt à signer côté client
            res.setStatut(ReservationStatus.DEPOSIT_HELD);

            // La caution est toujours en attente à ce stade
            if (res.getDepositStatus() == null) {
                res.setDepositStatus(DepositStatus.PENDING);
            }

            if (res.getDepositAmount() == null) {
                res.setDepositAmount(BigDecimal.ZERO);
            }

            String pdfPath = pdfService.generateContractPdf(res);
            RentalContract contract = RentalContract.builder()
                    .reservationLocation(res)
                    .contractPdfUrl(pdfPath)
                    .build();

            contractRepository.save(contract);
            res.setRentalContract(contract);
        } else {
            res.setLicenseStatus(LicenseStatus.REJECTED);
            res.setStatut(ReservationStatus.CANCELLED);
        }
        return reservationRepository.save(res);
    }


    @Override
    @Transactional
    public ReservationLocation signContract(Long id, String base64Signature, String signedBy) {
        ReservationLocation res = reservationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Réservation non trouvée"));

        RentalContract contract = res.getRentalContract();
        if (contract == null || contract.getContractPdfUrl() == null || contract.getContractPdfUrl().isBlank()) {
            throw new RuntimeException("Contrat introuvable pour cette réservation");
        }

        String finalPdf = pdfService.addSignatureToPdf(contract.getContractPdfUrl(), base64Signature, signedBy);

        String storedSignaturePath = saveBase64ImageIfNeeded(
                base64Signature,
                SIGNATURE_UPLOAD_DIR,
                "signature_" + id
        );

        contract.setSignatureImageUrl(storedSignaturePath); // chemin court en DB
        contract.setContractPdfUrl(finalPdf);
        contract.setDateSignature(LocalDateTime.now());
        contract.setSignedBy(signedBy);

        res.setStatut(ReservationStatus.CONTRACT_SIGNED);
        if (res.getDepositStatus() == null) {
            res.setDepositStatus(DepositStatus.PENDING);
        }
        return reservationRepository.save(res);
    }

    private String saveBase64ImageIfNeeded(String value, Path dir, String prefix) {
        if (value == null || value.isBlank()) {
            return value;
        }

        // Si ce n'est pas du data URL/base64, on suppose déjà un chemin/URL court.
        if (!value.startsWith("data:image/")) {
            return value;
        }
        try {
            Files.createDirectories(dir);

            String[] parts = value.split(",", 2);
            if (parts.length != 2) {
                throw new IllegalArgumentException("Format base64 invalide");
            }

            String meta = parts[0]; // ex: data:image/png;base64
            String payload = parts[1];

            String ext = "png";
            if (meta.contains("image/jpeg")) ext = "jpg";
            else if (meta.contains("image/webp")) ext = "webp";

            byte[] imageBytes = Base64.getDecoder().decode(payload);
            String fileName = prefix + "_" + System.currentTimeMillis() + "." + ext;
            Path target = dir.resolve(fileName);
            Files.write(target, imageBytes);

            // Stocker un chemin relatif en DB (portable)
            return target.toString().replace("\\", "/");
        } catch (Exception e) {
            throw new RuntimeException("Erreur sauvegarde image base64", e);
        }
    }
    @Override
    @Transactional
    public void checkInVehicle(Long id, List<String> photoUrls) {
        ReservationLocation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Réservation non trouvée"));

        if (photoUrls == null || photoUrls.isEmpty()) {
            throw new IllegalArgumentException("Au moins une photo est requise pour le check-in");
        }

        // Le check-in ne doit se faire qu'après confirmation
        if (reservation.getStatut() != ReservationStatus.CONFIRMED) {
            throw new IllegalStateException("La réservation doit être CONFIRMED avant la remise du véhicule");
        }

        // Optionnel mais recommandé: caution bloquée obligatoire avant remise
        if (reservation.getDepositStatus() != DepositStatus.HELD) {
            throw new IllegalStateException("La caution doit être HELD avant la remise du véhicule");
        }

        for (String photoUrl : photoUrls) {
            String storedPhotoPath = saveBase64ImageIfNeeded(
                    photoUrl,
                    ETAT_LIEUX_UPLOAD_DIR,
                    "checkin_" + id
            );

            EtatDesLieuxPhoto photo = EtatDesLieuxPhoto.builder()
                    .reservationLocation(reservation)
                    .photoUrl(storedPhotoPath)
                    .type("CHECK_IN")
                    .dateUpload(LocalDateTime.now())
                    .build();

            reservation.getEtatDesLieuxPhotos().add(photo);
        }
        // EN_COURS = IN_PROGRESS dans ton enum
        reservation.setStatut(ReservationStatus.IN_PROGRESS);
        reservation.setDateModification(LocalDateTime.now());

        reservationRepository.save(reservation);

        // TODO notification client (voir point 2)
    }
    @Override
    @Transactional
    public void checkOutVehicle(Long id, List<String> photoUrls) {
        ReservationLocation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Réservation non trouvée"));

        if (photoUrls == null || photoUrls.isEmpty()) {
            throw new IllegalArgumentException("Au moins une photo est requise pour le check-out");
        }

        for (String photoUrl : photoUrls) {
            String storedPhotoPath = saveBase64ImageIfNeeded(
                    photoUrl,
                    ETAT_LIEUX_UPLOAD_DIR,
                    "checkout_" + id
            );

            EtatDesLieuxPhoto photo = EtatDesLieuxPhoto.builder()
                    .reservationLocation(reservation)
                    .photoUrl(storedPhotoPath)
                    .type("CHECK_OUT")
                    .dateUpload(LocalDateTime.now())
                    .build();

            reservation.getEtatDesLieuxPhotos().add(photo);
        }
        // Fin de la location
        reservation.setStatut(ReservationStatus.COMPLETED);
        reservationRepository.save(reservation);
    }

    @Override
    public boolean isVehicleAvailable(Long vehiculeId, LocalDateTime start, LocalDateTime end) {
        // Utilise ta méthode existsBy... qui existe déjà dans le repository
        boolean isBooked = reservationRepository
                .existsByVehiculeAgence_IdVehiculeAgenceAndStatutInAndDateDebutLessThanEqualAndDateFinGreaterThanEqual(
                        vehiculeId,
                        List.of(ReservationStatus.CONFIRMED, ReservationStatus.IN_PROGRESS, ReservationStatus.ACTIVE),
                        start,
                        end);
        return !isBooked;
    }
    @Override
    public List<ReservationLocation> getReservationsByAgence(Long agenceId) {
        return reservationRepository.findByAgenceLocation_IdAgence(agenceId);
    }
    @Override
    @Transactional
    public ReservationLocation holdDeposit(Long id, String mode) {
        ReservationLocation res = reservationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Réservation non trouvée"));

        // idempotent: déjà traité
        if (res.getDepositStatus() == DepositStatus.HELD
                && res.getStatut() == ReservationStatus.DEPOSIT_HELD) {
            return res;
        }

        if (res.getStatut() != ReservationStatus.CONTRACT_SIGNED) {
            throw new IllegalStateException("Le contrat doit être signé avant de confirmer la caution");
        }

        res.setDepositStatus(DepositStatus.HELD);
        res.setStatut(ReservationStatus.DEPOSIT_HELD);
        res.setDateModification(LocalDateTime.now());

        // optionnel: logger le mode PHYSICAL/ONLINE
        // log.info("Deposit held for reservation {} via mode {}", id, mode);

        return reservationRepository.save(res);
    }
}