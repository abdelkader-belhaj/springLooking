package tn.hypercloud.service.transport;

import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.hypercloud.dto.transport.DriverNotificationDTO;
import tn.hypercloud.entity.transport.*;
import tn.hypercloud.entity.transport.enums.*;
import tn.hypercloud.entity.user.User;
import tn.hypercloud.repository.transport.*;
import tn.hypercloud.repository.user.UserRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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
    private final AnnulationLocationRepository annulationLocationRepository;
    private final RentalContractRepository contractRepository;
    private final PdfService pdfService;
    private final SimpMessagingTemplate messagingTemplate;
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

        BigDecimal prixJournalier = reservation.getVehiculeAgence().getPrixJour() != null
            ? reservation.getVehiculeAgence().getPrixJour()
            : BigDecimal.ZERO;

        // Backward-compatibility fallback for old vehicles not yet migrated.
        if (prixJournalier.compareTo(BigDecimal.ZERO) <= 0) {
            BigDecimal prixKm = reservation.getVehiculeAgence().getPrixKm() != null
                ? reservation.getVehiculeAgence().getPrixKm()
                : BigDecimal.ZERO;
            BigDecimal prixMinute = reservation.getVehiculeAgence().getPrixMinute() != null
                ? reservation.getVehiculeAgence().getPrixMinute().multiply(BigDecimal.valueOf(60))
                : BigDecimal.ZERO;
            BigDecimal tarifMinimal = BigDecimal.valueOf(45);
            prixJournalier = prixKm.max(prixMinute).max(tarifMinimal);
        }

        if (prixJournalier.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalStateException("Tarif journalier du véhicule invalide");
        }

        BigDecimal prixTotal = prixJournalier
            .multiply(BigDecimal.valueOf(days))
            .setScale(2, RoundingMode.HALF_UP);
        reservation.setPrixTotal(prixTotal);

        if (reservation.getAdvanceAmount() == null) {
            reservation.setAdvanceAmount(prixTotal.multiply(BigDecimal.valueOf(0.30)).setScale(2, RoundingMode.HALF_UP));
        }

        // Caution = 10% of vehicle price (prixVehicule)
        if (reservation.getDepositAmount() == null) {
            BigDecimal vehiclePrice = reservation.getVehiculeAgence().getPrixVehicule() != null
                    ? reservation.getVehiculeAgence().getPrixVehicule()
                    : BigDecimal.ZERO;
            reservation.setDepositAmount(vehiclePrice.multiply(BigDecimal.valueOf(0.10)).setScale(2, RoundingMode.HALF_UP));
        }

        if (reservation.getPaymentPhase() == null || reservation.getPaymentPhase().isBlank()) {
            reservation.setPaymentPhase("DRAFT");
        }

        if (reservation.getAdvanceStatus() == null || reservation.getAdvanceStatus().isBlank()) {
            reservation.setAdvanceStatus("PENDING");
        }

        if (reservation.getPaymentIntentId() != null && !reservation.getPaymentIntentId().isBlank()
                || "PAID".equalsIgnoreCase(reservation.getAdvanceStatus())) {
            reservation.setAdvanceStatus("PAID");
            reservation.setPaymentPhase("ADVANCE_PAID");
            reservation.setStatut(ReservationStatus.KYC_PENDING);
        } else {
            reservation.setStatut(ReservationStatus.DRAFT);
        }

        reservation.setDepositStatus(DepositStatus.PENDING);
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

        if (res.getStatut() == ReservationStatus.CONFIRMED) {
            return res; // idempotent
        }

        if (res.getDepositStatus() != DepositStatus.HELD) {
            throw new IllegalStateException("La caution doit être HELD avant confirmation");
        }

        if (res.getStatut() != ReservationStatus.CONTRACT_SIGNED) {
            throw new IllegalStateException("Le client doit signer le contrat avant confirmation");
        }

        res.setStatut(ReservationStatus.CONFIRMED);
        res.setPaymentPhase("CONFIRMED_PENDING_FINAL_PAYMENT");
        res.setDateModification(LocalDateTime.now());
        ReservationLocation saved = reservationRepository.save(res);
        notifyReservationWorkflowStep(
            saved,
            "RESERVATION_CONFIRMED",
            "Réservation confirmée",
            "La réservation #" + saved.getIdReservation() + " est confirmée. Paiement final attendu.",
            true,
            true,
            java.util.Map.of(
                "reservationId", saved.getIdReservation(),
                "reservationStatus", String.valueOf(saved.getStatut()),
                "paymentPhase", String.valueOf(saved.getPaymentPhase()),
                "nextStep", "FINAL_PAYMENT"
            )
        );
        return saved;
    }
    @Override
    @Transactional
    public ReservationLocation cancelReservation(Long id, String cancelledBy, String reason) {
        ReservationLocation res = getById(id);
        if (res == null) throw new RuntimeException("Réservation non trouvée");

        if (res.getStatut() == ReservationStatus.CANCELLED) {
            return res;
        }

        if (res.getStatut() == ReservationStatus.IN_PROGRESS
                || res.getStatut() == ReservationStatus.ACTIVE
                || res.getStatut() == ReservationStatus.COMPLETED) {
            throw new IllegalStateException("Annulation impossible après démarrage de la location");
        }

        AnnulationPar annulePar = parseAnnulationPar(cancelledBy);
        String cancellationReason = reason != null ? reason.trim() : null;
        boolean advancePaid = isAdvancePaid(res);
        BigDecimal advanceAmount = normalizeMoney(res.getAdvanceAmount());
        BigDecimal depositAmount = normalizeMoney(res.getDepositAmount());

        PhaseAnnulation phaseAnnulation;
        BigDecimal montantRembourse;
        BigDecimal montantPerdu;
        StatutRemboursement statutRemboursement;

        if (!advancePaid) {
            // Avant paiement initial: annulation simple, rien à rembourser.
            res.setPaymentPhase("CANCELLED_NO_REFUND");
            if (res.getDepositStatus() == null) {
                res.setDepositStatus(DepositStatus.PENDING);
            }
            phaseAnnulation = PhaseAnnulation.AVANT_PAIEMENT;
            montantRembourse = BigDecimal.ZERO;
            montantPerdu = BigDecimal.ZERO;
            statutRemboursement = StatutRemboursement.COMPLETED;
        } else if (res.getStatut() == ReservationStatus.CONFIRMED) {
            // Après confirmation: règles différentes selon qui annule
            if (annulePar == AnnulationPar.AGENCE) {
                // Agence annule après confirmation: remboursement total (avance + caution)
                applyFullRefundState(res, "CANCELLED_BY_AGENCY_REFUND_TOTAL");
                phaseAnnulation = PhaseAnnulation.APRES_CONFIRMATION;
                montantRembourse = advanceAmount.add(depositAmount).setScale(2, RoundingMode.HALF_UP);
                montantPerdu = BigDecimal.ZERO;
                statutRemboursement = StatutRemboursement.PENDING;
            } else {
                // Client annule après confirmation: caution remboursée, avance perdue
                res.setDepositStatus(DepositStatus.RELEASED);
                res.setPaymentPhase("CANCELLED_DEPOSIT_REFUNDED_ADVANCE_LOST");
                phaseAnnulation = PhaseAnnulation.APRES_CONFIRMATION;
                montantRembourse = depositAmount;
                montantPerdu = advanceAmount;
                statutRemboursement = StatutRemboursement.COMPLETED;
            }
        } else {
            // Après paiement initial mais avant confirmation: remboursement total (peu importe qui annule)
            applyFullRefundState(res, "CANCELLED_REFUNDED_TOTAL");
            phaseAnnulation = PhaseAnnulation.APRES_PAIEMENT;
            montantRembourse = advanceAmount.add(depositAmount).setScale(2, RoundingMode.HALF_UP);
            montantPerdu = BigDecimal.ZERO;
            statutRemboursement = StatutRemboursement.PENDING;
        }

        res.setStatut(ReservationStatus.CANCELLED);
        res.setDateModification(LocalDateTime.now());
        ReservationLocation saved = reservationRepository.save(res);

        saveCancellationRecord(
                saved,
                annulePar,
                phaseAnnulation,
                montantRembourse,
                montantPerdu,
                cancellationReason,
                statutRemboursement
        );

        if (annulePar == AnnulationPar.CLIENT) {
            notifyAgencyReservationCancelled(saved, montantRembourse, montantPerdu, statutRemboursement);
            // Aussi notifier le client de sa propre annulation avec détails clairs
            notifyClientAfterCancellation(saved, montantRembourse, montantPerdu);
        } else if (annulePar == AnnulationPar.AGENCE) {
            notifyClientReservationCancelled(saved, montantRembourse, montantPerdu, statutRemboursement, false);
        }

        return saved;
    }

    @Override
    @Transactional
    public ReservationLocation payAdvance(Long id, PaiementMethode methode, String paymentIntentId) {
        ReservationLocation reservation = getById(id);
        if (reservation == null) {
            throw new RuntimeException("Réservation non trouvée");
        }

        if (reservation.getStatut() == ReservationStatus.CANCELLED) {
            throw new IllegalStateException("Impossible de payer une réservation annulée");
        }

        if (reservation.getLicenseStatus() != LicenseStatus.APPROVED ||
                reservation.getStatut() != ReservationStatus.DEPOSIT_HELD) {
            throw new IllegalStateException("Le paiement initial est autorisé après validation du permis par l'agence");
        }

        BigDecimal advanceAmount = reservation.getAdvanceAmount();
        if (advanceAmount == null || advanceAmount.compareTo(BigDecimal.ZERO) <= 0) {
            advanceAmount = reservation.getPrixTotal()
                    .multiply(BigDecimal.valueOf(0.30))
                    .setScale(2, RoundingMode.HALF_UP);
            reservation.setAdvanceAmount(advanceAmount);
        }

        BigDecimal depositAmount = reservation.getDepositAmount();
        if (depositAmount == null || depositAmount.compareTo(BigDecimal.ZERO) <= 0) {
            BigDecimal vehiclePrice = reservation.getVehiculeAgence() != null
                && reservation.getVehiculeAgence().getPrixVehicule() != null
                ? reservation.getVehiculeAgence().getPrixVehicule()
                : BigDecimal.ZERO;

            depositAmount = vehiclePrice
                .multiply(BigDecimal.valueOf(0.10))
                .setScale(2, RoundingMode.HALF_UP);
            reservation.setDepositAmount(depositAmount);
        }

        reservation.setAdvanceStatus("PAID");
        reservation.setPaymentPhase("ADVANCE_PAID");
        reservation.setPaymentIntentId(paymentIntentId != null ? paymentIntentId.trim() : null);
        reservation.setStatut(ReservationStatus.DEPOSIT_HELD);
        // Advance + deposit are now taken in the same upfront transaction.
        reservation.setDepositStatus(DepositStatus.HELD);
        reservation.setDateModification(LocalDateTime.now());

        BigDecimal initialAmount = advanceAmount.add(depositAmount).setScale(2, RoundingMode.HALF_UP);
        Optional<PaiementTransport> existingInitial = paiementRepository
            .findByReservationLocationAndPhasePaiement(reservation, PaiementReservationPhase.INITIAL);
        if (existingInitial.isEmpty()) {
            PaiementTransport initialPayment = PaiementTransport.builder()
                .reservationLocation(reservation)
                .montantTotal(initialAmount)
                .methode(methode)
                .statut(PaiementStatut.COMPLETED)
                .phasePaiement(PaiementReservationPhase.INITIAL)
                .datePaiement(LocalDateTime.now())
                .build();
            paiementRepository.save(initialPayment);
        }

            ReservationLocation saved = reservationRepository.save(reservation);
            notifyReservationWorkflowStep(
                saved,
                "ADVANCE_PAID",
                "Paiement initial confirmé",
                "Le paiement initial de la réservation #" + saved.getIdReservation() + " est confirmé. Le contrat peut être signé.",
                true,
                true,
                java.util.Map.of(
                    "reservationId", saved.getIdReservation(),
                    "reservationStatus", String.valueOf(saved.getStatut()),
                    "paymentPhase", String.valueOf(saved.getPaymentPhase()),
                    "nextStep", "SIGN_CONTRACT"
                )
            );
            return saved;
    }

    @Override
    @Transactional
    public ReservationLocation completeReservation(Long id, PaiementMethode methode, String paymentIntentId) {
        // === CODE PAIEMENT + WALLET TEL QU'IL EXISTE DANS TON PROJET ===
        ReservationLocation reservation = getById(id);
        if (reservation == null) throw new RuntimeException("Réservation non trouvée");

        if ("FINAL_PAID".equalsIgnoreCase(reservation.getPaymentPhase())) {
            return reservation; // idempotent
        }

        if (!"PAID".equalsIgnoreCase(reservation.getAdvanceStatus())) {
            throw new IllegalStateException("L'avance de réservation doit être payée avant le paiement final");
        }

        if (reservation.getStatut() != ReservationStatus.CONFIRMED) {
            throw new IllegalStateException("Le paiement final est autorisé après confirmation agence et avant check-in");
        }

        BigDecimal advanceAmount = reservation.getAdvanceAmount() != null
                ? reservation.getAdvanceAmount()
                : BigDecimal.ZERO;
        BigDecimal remainingAmount = reservation.getPrixTotal()
                .subtract(advanceAmount)
                .max(BigDecimal.ZERO)
                .setScale(2, RoundingMode.HALF_UP);

        Optional<PaiementTransport> existingFinalPayment = paiementRepository
                .findByReservationLocationAndPhasePaiement(reservation, PaiementReservationPhase.FINAL);

        PaiementTransport paiementTransport;
        if (existingFinalPayment.isPresent()) {
            paiementTransport = existingFinalPayment.get();
            paiementTransport.setMontantTotal(remainingAmount);
            paiementTransport.setMethode(methode);
            paiementTransport.setStatut(PaiementStatut.COMPLETED);
            paiementTransport.setDatePaiement(LocalDateTime.now());
            paiementTransport.calculerCommission();
            paiementTransport = paiementRepository.save(paiementTransport);
        } else {
            List<PaiementTransport> reservationPayments = paiementRepository
                    .findByReservationLocation(reservation);

            if (!reservationPayments.isEmpty()) {
                // Legacy DB fallback: if unique constraint still exists on id_reservation_location,
                // reuse existing row and promote it to FINAL phase instead of inserting a 2nd row.
                paiementTransport = reservationPayments.get(0);
                paiementTransport.setMontantTotal(remainingAmount);
                paiementTransport.setMethode(methode);
                paiementTransport.setStatut(PaiementStatut.COMPLETED);
                paiementTransport.setPhasePaiement(PaiementReservationPhase.FINAL);
                paiementTransport.setDatePaiement(LocalDateTime.now());
                paiementTransport.calculerCommission();
                paiementTransport = paiementRepository.save(paiementTransport);
            } else {
                paiementTransport = PaiementTransport.builder()
                        .reservationLocation(reservation)
                        .montantTotal(remainingAmount)
                        .methode(methode)
                        .statut(PaiementStatut.COMPLETED)
                        .phasePaiement(PaiementReservationPhase.FINAL)
                        .datePaiement(LocalDateTime.now())
                        .build();

                paiementTransport = paiementRepository.save(paiementTransport);
            }
        }

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
            reservation.setPaymentIntentId(paymentIntentId != null ? paymentIntentId.trim() : reservation.getPaymentIntentId());
            reservation.setPaymentPhase("FINAL_PAID");
        reservation.setDateModification(LocalDateTime.now());

        ReservationLocation saved = reservationRepository.save(reservation);
        notifyReservationWorkflowStep(
            saved,
            "FINAL_PAID",
            "Paiement final confirmé",
            "Le paiement final de la réservation #" + saved.getIdReservation() + " est confirmé. Le check-in est maintenant possible.",
            true,
            true,
            java.util.Map.of(
                "reservationId", saved.getIdReservation(),
                "reservationStatus", String.valueOf(saved.getStatut()),
                "paymentPhase", String.valueOf(saved.getPaymentPhase()),
                "nextStep", "CHECK_IN"
            )
        );
        return saved;
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
        ReservationLocation saved = reservationRepository.save(res);
        notifyReservationWorkflowStep(
            saved,
            "LICENSE_UPLOADED",
            "Permis soumis",
            "Le client a soumis son permis pour la réservation #" + saved.getIdReservation() + ". Vérification agence requise.",
            false,
            true,
            java.util.Map.of(
                "reservationId", saved.getIdReservation(),
                "reservationStatus", String.valueOf(saved.getStatut()),
                "paymentPhase", String.valueOf(saved.getPaymentPhase()),
                "nextStep", "LICENSE_REVIEW"
            )
        );
        return saved;
    }
    @Override
    @Transactional
    public ReservationLocation approveLicense(Long id, boolean approved, String reason) {
        ReservationLocation res = reservationRepository.findById(id).orElseThrow();

        if (res.getStatut() == ReservationStatus.CANCELLED) {
            throw new IllegalStateException("Impossible de traiter le permis d'une réservation annulée");
        }

        if (approved) {
            res.setLicenseStatus(LicenseStatus.APPROVED);

            // Contrat prêt à signer côté client
            res.setStatut(ReservationStatus.DEPOSIT_HELD);
            res.setPaymentPhase("VERIFICATION_PENDING");

            // Tant que le paiement initial (avance + caution) n'est pas confirmé,
            // la caution reste en attente et ne doit pas être considérée bloquée.
            if (!"PAID".equalsIgnoreCase(res.getAdvanceStatus())) {
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
                ReservationLocation saved = reservationRepository.save(res);
                notifyReservationWorkflowStep(
                    saved,
                    "LICENSE_APPROVED",
                    "Permis validé",
                    "Votre permis a été validé pour la réservation #" + saved.getIdReservation() + ". Signature du contrat requise.",
                    true,
                    false,
                    java.util.Map.of(
                        "reservationId", saved.getIdReservation(),
                        "reservationStatus", String.valueOf(saved.getStatut()),
                        "paymentPhase", String.valueOf(saved.getPaymentPhase()),
                        "nextStep", "SIGN_CONTRACT"
                    )
                );
                return saved;
        } else {
            // Rejet du permis : le client peut soumettre un nouveau permis
            res.setLicenseStatus(LicenseStatus.REJECTED);
            String normalizedReason = reason != null ? reason.trim() : null;
            res.setLicenseRejectionReason(normalizedReason);
            res.setNote(normalizedReason);

            // Ne pas annuler la réservation, juste rejeter le permis
            // La réservation reste en attente de vérification du permis
            // Phase de paiement ne change pas, client peut renvoyer un permis
            if (res.getPaymentPhase() == null || res.getPaymentPhase().isEmpty()) {
                res.setPaymentPhase("ADVANCE_PENDING");
            }

            ReservationLocation saved = reservationRepository.save(res);

            // Notifier le client et l'agence : permis rejeté, le client peut renvoyer
            notifyReservationWorkflowStep(
                    saved,
                    "LICENSE_REJECTED",
                    "Permis rejeté",
                    "Votre permis a été rejeté pour la raison suivante: " + normalizedReason + 
                    ". Veuillez soumettre un nouveau permis.",
                    true,
                    false,
                    java.util.Map.of(
                        "reservationId", saved.getIdReservation(),
                        "reason", normalizedReason != null ? normalizedReason : "",
                        "nextStep", "RESUBMIT_LICENSE"
                    )
            );

            // Notifier l'agence qu'un nouveau permis peut être soumis
            notifyReservationWorkflowStep(
                    saved,
                    "LICENSE_RESUBMISSION_PENDING",
                    "Permis en attente de resoumission",
                    "Le client peut renvoyer un permis pour la réservation #" + saved.getIdReservation(),
                    false,
                    true,
                    java.util.Map.of(
                        "reservationId", saved.getIdReservation(),
                        "reason", normalizedReason != null ? normalizedReason : "",
                        "nextStep", "WAIT_FOR_LICENSE_RESUBMISSION"
                    )
            );

            return saved;
        }
    }

    private void saveCancellationRecord(
            ReservationLocation reservation,
            AnnulationPar annulePar,
            PhaseAnnulation phaseAnnulation,
            BigDecimal montantRembourse,
            BigDecimal montantPerdu,
            String raison,
            StatutRemboursement statutRemboursement
    ) {
        AnnulationLocation annulation = AnnulationLocation.builder()
                .reservation(reservation)
                .annulePar(annulePar)
                .phaseAnnulation(phaseAnnulation)
                .montantRembourse(normalizeMoney(montantRembourse))
                .montantPerdu(normalizeMoney(montantPerdu))
                .raison(raison)
                .statutRemboursement(statutRemboursement)
                .build();
        annulationLocationRepository.save(annulation);
    }

    private AnnulationPar parseAnnulationPar(String value) {
        if (value == null || value.isBlank()) {
            return AnnulationPar.CLIENT;
        }

        try {
            return AnnulationPar.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return AnnulationPar.CLIENT;
        }
    }

    private BigDecimal normalizeMoney(BigDecimal value) {
        return (value == null ? BigDecimal.ZERO : value).setScale(2, RoundingMode.HALF_UP);
    }

    private void notifyAgencyReservationCancelled(
            ReservationLocation reservation,
            BigDecimal montantRembourse,
            BigDecimal montantPerdu,
            StatutRemboursement statut
    ) {
        try {
            User agencyUser = resolveAgencyUser(reservation);
            if (agencyUser == null || agencyUser.getEmail() == null || agencyUser.getEmail().isBlank()) {
                return;
            }

            BigDecimal refunded = normalizeMoney(montantRembourse);
            BigDecimal lost = normalizeMoney(montantPerdu);
            String refundMessage;
            if (refunded.compareTo(BigDecimal.ZERO) > 0 && lost.compareTo(BigDecimal.ZERO) > 0) {
                // Client annule après confirmation: caution remboursée, avance perdue (l'agence retient l'avance)
                refundMessage = " Caution remboursée au client: " + refunded + " TND. Avance conservée: " + lost + " TND";
            } else if (refunded.compareTo(BigDecimal.ZERO) > 0) {
                // Remboursement total
                refundMessage = " Remboursement total au client: " + refunded + " TND";
            } else {
                refundMessage = " Aucun remboursement.";
            }

            DriverNotificationDTO notification = DriverNotificationDTO.builder()
                    .type("RESERVATION_CANCELLED_BY_CLIENT")
                    .titre("Réservation annulée par le client")
                    .message("Le client a annulé la réservation #" + reservation.getIdReservation() + "." + refundMessage)
                    .data(Map.of(
                            "reservationId", reservation.getIdReservation(),
                            "refundAmount", refunded,
                            "lostAmount", lost,
                            "refundStatus", String.valueOf(statut)
                    ))
                    .build();

            messagingTemplate.convertAndSendToUser(
                    agencyUser.getEmail(),
                    "/queue/notifications",
                    notification
            );

            if (agencyUser.getId() != null) {
                messagingTemplate.convertAndSend(
                        "/topic/user/" + agencyUser.getId() + "/notifications",
                        notification
                );
            }
        } catch (Exception ignored) {
        }
    }

    private void notifyClientReservationCancelled(
            ReservationLocation reservation,
            BigDecimal montantRembourse,
            BigDecimal montantPerdu,
            StatutRemboursement statut,
            boolean fromAgencyRefusal
    ) {
        try {
            User client = reservation.getClient();
            if (client == null && reservation.getClientId() != null) {
                client = userRepository.findById(reservation.getClientId()).orElse(null);
            }

            if (client == null || client.getEmail() == null || client.getEmail().isBlank()) {
                return;
            }

            BigDecimal refunded = normalizeMoney(montantRembourse);
            BigDecimal lost = normalizeMoney(montantPerdu);
            String refundMessage;
            if (refunded.compareTo(BigDecimal.ZERO) > 0 && lost.compareTo(BigDecimal.ZERO) > 0) {
                // Cas annulation après confirmation par client: caution remboursée, avance perdue
                refundMessage = "Caution remboursée: " + refunded + " TND | Avance non remboursée: " + lost + " TND";
            } else if (refunded.compareTo(BigDecimal.ZERO) > 0) {
                // Remboursement total
                refundMessage = "Remboursement total: " + refunded + " TND (avance + caution)";
                if (lost.compareTo(BigDecimal.ZERO) > 0) {
                    refundMessage += " | Montant non remboursé: " + lost + " TND";
                }
            } else if (lost.compareTo(BigDecimal.ZERO) > 0) {
                refundMessage = "Montant non remboursé: " + lost + " TND";
            } else {
                refundMessage = "Aucun remboursement à traiter.";
            }

            String baseMessage = fromAgencyRefusal
                    ? "Votre réservation #" + reservation.getIdReservation() + " a été annulée après refus du permis."
                    : "Votre réservation #" + reservation.getIdReservation() + " a été annulée par l'agence.";

            DriverNotificationDTO notification = DriverNotificationDTO.builder()
                    .type(fromAgencyRefusal ? "LICENSE_REJECTED_REFUND" : "RESERVATION_CANCELLED_BY_AGENCY")
                    .titre(fromAgencyRefusal ? "Permis refusé par l'agence" : "Réservation annulée par l'agence")
                    .message(baseMessage + " " + refundMessage)
                    .data(Map.of(
                            "reservationId", reservation.getIdReservation(),
                            "refundAmount", refunded,
                            "lostAmount", lost,
                            "refundStatus", String.valueOf(statut)
                    ))
                    .build();

            messagingTemplate.convertAndSendToUser(
                    client.getEmail(),
                    "/queue/notifications",
                    notification
            );

            if (client.getId() != null) {
                messagingTemplate.convertAndSend(
                        "/topic/user/" + client.getId() + "/notifications",
                        notification
                );
            }
        } catch (Exception ignored) {
        }
    }

    private void notifyClientAfterCancellation(
            ReservationLocation reservation,
            BigDecimal montantRembourse,
            BigDecimal montantPerdu
    ) {
        try {
            User client = reservation.getClient();
            if (client == null && reservation.getClientId() != null) {
                client = userRepository.findById(reservation.getClientId()).orElse(null);
            }

            if (client == null || client.getEmail() == null || client.getEmail().isBlank()) {
                return;
            }

            BigDecimal refunded = normalizeMoney(montantRembourse);
            BigDecimal lost = normalizeMoney(montantPerdu);
            
            String refundMessage = "";
            if (refunded.compareTo(BigDecimal.ZERO) > 0 && lost.compareTo(BigDecimal.ZERO) > 0) {
                // Client annule après confirmation: caution remboursée, avance perdue
                refundMessage = "Caution remboursée: " + refunded + " TND | Avance non remboursée: " + lost + " TND";
            } else if (refunded.compareTo(BigDecimal.ZERO) > 0) {
                // Remboursement total
                refundMessage = "Remboursement total: " + refunded + " TND (avance + caution)";
            } else {
                refundMessage = "Aucun remboursement.";
            }

            DriverNotificationDTO notification = DriverNotificationDTO.builder()
                    .type("RESERVATION_CANCELLED_BY_CLIENT")
                    .titre("Annulation de réservation confirmée")
                    .message("Votre réservation #" + reservation.getIdReservation() + " a été annulée. " + refundMessage)
                    .data(Map.of(
                            "reservationId", reservation.getIdReservation(),
                            "refundAmount", refunded,
                            "lostAmount", lost
                    ))
                    .build();

            messagingTemplate.convertAndSendToUser(
                    client.getEmail(),
                    "/queue/notifications",
                    notification
            );

            if (client.getId() != null) {
                messagingTemplate.convertAndSend(
                        "/topic/user/" + client.getId() + "/notifications",
                        notification
                );
            }
        } catch (Exception ignored) {
        }
    }

    private User resolveAgencyUser(ReservationLocation reservation) {
        if (reservation == null) {
            return null;
        }

        if (reservation.getAgenceLocation() != null && reservation.getAgenceLocation().getUtilisateur() != null) {
            return reservation.getAgenceLocation().getUtilisateur();
        }

        if (reservation.getVehiculeAgence() != null
                && reservation.getVehiculeAgence().getAgence() != null
                && reservation.getVehiculeAgence().getAgence().getUtilisateur() != null) {
            return reservation.getVehiculeAgence().getAgence().getUtilisateur();
        }

        return null;
    }

    private boolean isAdvancePaid(ReservationLocation reservation) {
        if (reservation == null) {
            return false;
        }

        String advanceStatus = String.valueOf(reservation.getAdvanceStatus() == null ? "" : reservation.getAdvanceStatus()).trim();
        if ("PAID".equalsIgnoreCase(advanceStatus)) {
            return true;
        }

        String phase = String.valueOf(reservation.getPaymentPhase() == null ? "" : reservation.getPaymentPhase()).trim();
        return "ADVANCE_PAID".equalsIgnoreCase(phase)
                || "CONFIRMED_PENDING_FINAL_PAYMENT".equalsIgnoreCase(phase)
                || "FINAL_PAID".equalsIgnoreCase(phase);
    }

    private void applyFullRefundState(ReservationLocation reservation, String phaseLabel) {
        reservation.setDepositStatus(DepositStatus.RELEASED);
        reservation.setAdvanceStatus("REFUNDED");
        reservation.setPaymentPhase(phaseLabel);
        markInitialPaymentAsRefunded(reservation);
    }

    private void markInitialPaymentAsRefunded(ReservationLocation reservation) {
        Optional<PaiementTransport> initialPayment = paiementRepository
                .findByReservationLocationAndPhasePaiement(reservation, PaiementReservationPhase.INITIAL);

        PaiementTransport paymentToRefund = initialPayment.orElseGet(() -> {
            List<PaiementTransport> reservationPayments = paiementRepository.findByReservationLocation(reservation);
            return reservationPayments.isEmpty() ? null : reservationPayments.get(0);
        });

        if (paymentToRefund == null) {
            return;
        }

        paymentToRefund.setStatut(PaiementStatut.REFUNDED);
        paiementRepository.save(paymentToRefund);
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
        ReservationLocation saved = reservationRepository.save(res);
        notifyReservationWorkflowStep(
            saved,
            "CONTRACT_SIGNED",
            "Contrat signé",
            "Le contrat de la réservation #" + saved.getIdReservation() + " a été signé. Confirmation agence possible.",
            true,
            true,
            java.util.Map.of(
                "reservationId", saved.getIdReservation(),
                "reservationStatus", String.valueOf(saved.getStatut()),
                "paymentPhase", String.valueOf(saved.getPaymentPhase()),
                "nextStep", "AGENCY_CONFIRMATION"
            )
        );
        return saved;
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

        if (!"FINAL_PAID".equalsIgnoreCase(reservation.getPaymentPhase())) {
            throw new IllegalStateException("Le client doit compléter le paiement final avant le check-in");
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

        ReservationLocation saved = reservationRepository.save(reservation);

        notifyReservationWorkflowStep(
            saved,
            "CHECK_IN",
            "Check-in validé",
            "Le check-in de la réservation #" + saved.getIdReservation() + " est validé. La location est en cours.",
            true,
            true,
            java.util.Map.of(
                "reservationId", saved.getIdReservation(),
                "reservationStatus", String.valueOf(saved.getStatut()),
                "paymentPhase", String.valueOf(saved.getPaymentPhase()),
                "nextStep", "CHECK_OUT"
            )
        );

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
        // Refund is now a separate agency-confirmed action.
        reservation.setDepositStatus(DepositStatus.HELD);
        reservation.setDateModification(LocalDateTime.now());
        ReservationLocation saved = reservationRepository.save(reservation);

        notifyReservationWorkflowStep(
            saved,
            "CHECK_OUT",
            "Check-out validé",
            "Le check-out de la réservation #" + saved.getIdReservation() + " est validé. Remboursement de caution possible.",
            true,
            true,
            java.util.Map.of(
                "reservationId", saved.getIdReservation(),
                "reservationStatus", String.valueOf(saved.getStatut()),
                "paymentPhase", String.valueOf(saved.getPaymentPhase()),
                "nextStep", "DEPOSIT_REFUND"
            )
        );
    }

    @Override
    @Transactional
    public ReservationLocation refundDeposit(Long id, PaiementMethode methode, String paymentIntentId) {
        ReservationLocation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Réservation non trouvée"));

        if (reservation.getStatut() != ReservationStatus.COMPLETED) {
            throw new IllegalStateException("Le remboursement de caution est possible uniquement après check-out");
        }

        if (reservation.getDepositStatus() == DepositStatus.RELEASED) {
            return reservation; // idempotent
        }

        if (reservation.getDepositStatus() != DepositStatus.HELD) {
            throw new IllegalStateException("La caution doit être HELD avant remboursement");
        }

        reservation.setDepositStatus(DepositStatus.RELEASED);
        reservation.setDateModification(LocalDateTime.now());
        ReservationLocation saved = reservationRepository.save(reservation);

        notifyClientDepositReleased(saved);
        notifyReservationWorkflowStep(
                saved,
                "DEPOSIT_RELEASED",
                "Caution remboursée",
                "La caution de la réservation #" + saved.getIdReservation() + " a été remboursée.",
                true,
                true,
                java.util.Map.of(
                        "reservationId", saved.getIdReservation(),
                        "reservationStatus", String.valueOf(saved.getStatut()),
                        "paymentPhase", String.valueOf(saved.getPaymentPhase()),
                        "nextStep", "COMPLETED"
                )
        );
        return saved;
    }

    private void notifyReservationWorkflowStep(
            ReservationLocation reservation,
            String type,
            String titre,
            String message,
            boolean notifyClient,
            boolean notifyAgency,
            Map<String, Object> additionalData
    ) {
        if (reservation == null) {
            return;
        }

        Map<String, Object> data = new java.util.HashMap<>();
        data.put("reservationId", reservation.getIdReservation());
        data.put("reservationStatus", String.valueOf(reservation.getStatut()));
        data.put("paymentPhase", String.valueOf(reservation.getPaymentPhase()));
        if (additionalData != null) {
            data.putAll(additionalData);
        }

        DriverNotificationDTO notification = DriverNotificationDTO.builder()
                .type(type)
                .titre(titre)
                .message(message)
                .data(data)
                .build();

        if (notifyClient) {
            sendReservationNotificationToUser(reservation.getClient(), notification);
        }

        if (notifyAgency) {
            sendReservationNotificationToUser(resolveAgencyUser(reservation), notification);
        }
    }

    private void sendReservationNotificationToUser(User user, DriverNotificationDTO notification) {
        if (user == null) {
            return;
        }

        if (user.getEmail() != null && !user.getEmail().isBlank()) {
            messagingTemplate.convertAndSendToUser(
                    user.getEmail(),
                    "/queue/notifications",
                    notification
            );
        }

        if (user.getId() != null) {
            messagingTemplate.convertAndSend(
                    "/topic/user/" + user.getId() + "/notifications",
                    notification
            );
        }
    }

    private void notifyClientDepositReleased(ReservationLocation reservation) {
        try {
            User client = reservation.getClient();
            if (client == null && reservation.getClientId() != null) {
                client = userRepository.findById(reservation.getClientId()).orElse(null);
            }

            if (client == null || client.getEmail() == null || client.getEmail().isBlank()) {
                return;
            }

            DriverNotificationDTO notification = DriverNotificationDTO.builder()
                    .type("DEPOSIT_RELEASED")
                    .titre("Caution remboursée")
                    .message("La caution de votre réservation #" + reservation.getIdReservation() + " a été remboursée.")
                    .data(java.util.Map.of(
                            "reservationId", reservation.getIdReservation(),
                            "depositStatus", String.valueOf(reservation.getDepositStatus()),
                            "amount", reservation.getDepositAmount()
                    ))
                    .build();

            // Primary path for authenticated user destinations.
            messagingTemplate.convertAndSendToUser(
                    client.getEmail(),
                    "/queue/notifications",
                    notification
            );

            // Fallback path when STOMP principal mapping is not established.
            if (client.getId() != null) {
                messagingTemplate.convertAndSend(
                        "/topic/user/" + client.getId() + "/notifications",
                        notification
                );
            }
        } catch (Exception ignored) {
            // Best effort: checkout must not fail if realtime notification cannot be delivered.
        }
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
        return reservationRepository.findAllByAgenceId(agenceId);
    }
    @Override
    @Transactional
    public ReservationLocation holdDeposit(Long id, String mode) {
        ReservationLocation res = reservationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Réservation non trouvée"));

        // idempotent: déjà traité
        if (res.getDepositStatus() == DepositStatus.HELD) {
            return res;
        }

        if (res.getStatut() != ReservationStatus.CONTRACT_SIGNED) {
            throw new IllegalStateException("Le contrat doit être signé avant de confirmer la caution");
        }

        res.setDepositStatus(DepositStatus.HELD);
        res.setStatut(ReservationStatus.DEPOSIT_HELD);
        res.setPaymentPhase("VERIFICATION_PENDING");
        res.setDateModification(LocalDateTime.now());

        // optionnel: logger le mode PHYSICAL/ONLINE
        // log.info("Deposit held for reservation {} via mode {}", id, mode);

        return reservationRepository.save(res);
    }
}