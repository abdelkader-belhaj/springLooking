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
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
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

        reservation.setStatut(ReservationStatus.PENDING);

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
    public ReservationLocation uploadLicense(Long id, String numeroPermis, String licenseImageUrl, LocalDateTime expiry) {
        ReservationLocation res = reservationRepository.findById(id).orElseThrow();
        res.setNumeroPermis(numeroPermis);
        res.setLicenseImageUrl(licenseImageUrl);
        res.setLicenseExpiryDate(expiry);
        res.setLicenseStatus(LicenseStatus.PENDING);
        return reservationRepository.save(res);
    }

    @Override
    @Transactional
    public ReservationLocation approveLicense(Long id, boolean approved, String reason) {
        ReservationLocation res = reservationRepository.findById(id).orElseThrow();

        if (approved) {
            res.setLicenseStatus(LicenseStatus.APPROVED);
            res.setStatut(ReservationStatus.DEPOSIT_HELD);

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
        ReservationLocation res = reservationRepository.findById(id).orElseThrow();
        RentalContract contract = res.getRentalContract();

        String finalPdf = pdfService.addSignatureToPdf(contract.getContractPdfUrl(), base64Signature, signedBy);

        contract.setSignatureImageUrl(base64Signature);
        contract.setContractPdfUrl(finalPdf);
        contract.setDateSignature(LocalDateTime.now());
        contract.setSignedBy(signedBy);

        res.setStatut(ReservationStatus.CONTRACT_SIGNED);
        res.setStatut(ReservationStatus.CONFIRMED);

        return reservationRepository.save(res);
    }

    @Override
    @Transactional
    public void checkInVehicle(Long id, List<String> photoUrls) {
        ReservationLocation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Réservation non trouvée"));

        if (photoUrls == null || photoUrls.isEmpty()) {
            throw new IllegalArgumentException("Au moins une photo est requise pour le check-in");
        }

        for (String photoUrl : photoUrls) {
            EtatDesLieuxPhoto photo = EtatDesLieuxPhoto.builder()
                    .reservationLocation(reservation)
                    .photoUrl(photoUrl)
                    .type("CHECK_IN")           // départ
                    .dateUpload(LocalDateTime.now())
                    .build();
            // On sauvegarde directement (pas besoin de repository séparé car cascade)
            reservation.getEtatDesLieuxPhotos().add(photo);
        }

        // Optionnel : changer le statut
        reservation.setStatut(ReservationStatus.ACTIVE);
        reservationRepository.save(reservation);
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
            EtatDesLieuxPhoto photo = EtatDesLieuxPhoto.builder()
                    .reservationLocation(reservation)
                    .photoUrl(photoUrl)
                    .type("CHECK_OUT")          // retour
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
}