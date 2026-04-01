package tn.hypercloud.service.transport;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.hypercloud.entity.transport.*;
import tn.hypercloud.entity.transport.enums.PaiementMethode;
import tn.hypercloud.entity.transport.enums.PaiementStatut;
import tn.hypercloud.entity.transport.enums.ReservationStatus;
import tn.hypercloud.entity.transport.enums.TransactionType;
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
    private final IPaiementService paiementService;
    private final PaiementRepository paiementRepository;
    private final AgenceLocationRepository agenceLocationRepository;       // ← NOUVEAU
    private final WalletTransactionRepository walletTransactionRepository; // ← NOUVEAU

    @Override
    @Transactional
    public ReservationLocation createReservation(ReservationLocation reservation) {

        // Résoudre le client via transient ID
        if (reservation.getClientId() != null) {
            tn.hypercloud.entity.user.User client = userRepository.findById(reservation.getClientId())
                    .orElseThrow(() -> new RuntimeException("Client non trouvé"));
            reservation.setClient(client);
        }

        // Résoudre le véhicule d'agence via transient ID
        if (reservation.getVehiculeAgenceId() != null) {
            VehiculeAgence vehicule = vehiculeAgenceRepository.findById(reservation.getVehiculeAgenceId())
                    .orElseThrow(() -> new RuntimeException("Véhicule d'agence non trouvé"));
            reservation.setVehiculeAgence(vehicule);
        }

        // Vérifier disponibilité
        boolean isBooked = reservationRepository.existsByVehiculeAgence_IdVehiculeAgenceAndStatutInAndDateDebutLessThanEqualAndDateFinGreaterThanEqual(
                reservation.getVehiculeAgence().getIdVehiculeAgence(),
                List.of(ReservationStatus.CONFIRMED, ReservationStatus.IN_PROGRESS),
                reservation.getDateDebut(),
                reservation.getDateFin());

        if (isBooked) {
            throw new IllegalStateException("Véhicule déjà réservé sur cette période");
        }

        // Calcul prix total (simple : nombre de jours × prixKm)
        long days = ChronoUnit.DAYS.between(reservation.getDateDebut(), reservation.getDateFin());
        if (days <= 0) throw new IllegalArgumentException("Date de fin invalide");

        BigDecimal prixTotal = reservation.getVehiculeAgence().getPrixKm().multiply(BigDecimal.valueOf(days));
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

        // === MISE À JOUR SOLDE AGENCE + TRANSACTION ===
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
}