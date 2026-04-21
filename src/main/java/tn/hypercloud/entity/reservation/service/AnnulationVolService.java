package tn.hypercloud.entity.reservation.service;

import com.stripe.exception.StripeException;
import com.stripe.model.Refund;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tn.hypercloud.entity.reservation.*;
import tn.hypercloud.entity.reservation.dto.ReservationResponse;
import tn.hypercloud.repository.reservation.PaiementVolRepository;
import tn.hypercloud.repository.reservation.ReservationVolRepository;
import tn.hypercloud.repository.reservation.VolRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AnnulationVolService {

    private final ReservationVolRepository reservationRepo;
    private final VolRepository volRepo;
    private final PaiementVolRepository paiementRepo;
    private final VolService volService;
    private final StripeService stripeService;
    private final EmailService emailService;

    // ============================================================
    //  CLIENT : DEMANDER ANNULATION + REMBOURSEMENT STRIPE RÉEL
    // ============================================================
    public ReservationResponse demanderAnnulation(String email, Integer reservationId) {

        ReservationVol res = reservationRepo.findById(reservationId)
                .orElseThrow(() -> new RuntimeException("Réservation introuvable"));

        if (!res.getTouriste().getEmail().equals(email))
            throw new RuntimeException("Accès refusé");

        if (res.getStatutReservation() != ReservationVol.StatutReservation.active)
            throw new RuntimeException("Cette réservation est déjà annulée");

        boolean estPayee = res.getPaiement() != null &&
                res.getPaiement().getStatut() == PaiementVol.StatutPaiement.paye;

        if (!estPayee)
            throw new RuntimeException(
                    "Seules les réservations payées peuvent être annulées. " +
                            "Statut actuel : " + (res.getPaiement() != null
                            ? res.getPaiement().getStatut()
                            : "aucun paiement")
            );

        // -------------------------------------------------------
        //  RÈGLE 48H
        // -------------------------------------------------------
        LocalDateTime departDateTime = res.getVolAller()
                .getDateDepart()
                .atTime(res.getVolAller().getHeureDepart());

        LocalDateTime limite = departDateTime.minusHours(48);

        if (LocalDateTime.now().isAfter(limite)) {
            throw new RuntimeException(
                    "Annulation impossible : le vol départ le " +
                            res.getVolAller().getDateDepart() +
                            " à " + res.getVolAller().getHeureDepart() +
                            ". L'annulation n'est autorisée que 48h avant le départ."
            );
        }

        // -------------------------------------------------------
        //  REMBOURSEMENT STRIPE RÉEL
        // -------------------------------------------------------
        String paymentIntentId = res.getPaiement().getReferenceTx();

        if (paymentIntentId != null && paymentIntentId.startsWith("pi_")) {
            try {
                Refund refund = stripeService.rembourserPaiement(paymentIntentId, null);
                if (!"succeeded".equals(refund.getStatus())) {
                    throw new RuntimeException(
                            "Remboursement Stripe non confirmé. Statut : " + refund.getStatus());
                }
            } catch (StripeException e) {
                throw new RuntimeException("Remboursement Stripe échoué : " + e.getMessage());
            }
        }

        // -------------------------------------------------------
        //  Restituer les places
        // -------------------------------------------------------
        Vol volAller = res.getVolAller();
        volAller.setPlaces(volAller.getPlaces() + res.getNbPassagers());
        volRepo.save(volAller);

        if (res.getVolRetour() != null) {
            Vol volRetour = res.getVolRetour();
            volRetour.setPlaces(volRetour.getPlaces() + res.getNbPassagers());
            volRepo.save(volRetour);
        }

        // -------------------------------------------------------
        //  Mettre à jour statuts
        // -------------------------------------------------------
        res.getPaiement().setStatut(PaiementVol.StatutPaiement.rembourse);
        res.getPaiement().setDatePaiement(LocalDateTime.now());
        paiementRepo.save(res.getPaiement());

        res.setStatutReservation(ReservationVol.StatutReservation.annulee);
        ReservationVol resSauvegardee = reservationRepo.save(res);

        // ← CORRECTION : email après sauvegarde complète
        emailService.envoyerEmailAnnulation(resSauvegardee);

        return toResponse(resSauvegardee);
    }

    // ============================================================
    //  SOCIÉTÉ : CONFIRMER REMBOURSEMENT MANUELLEMENT
    // ============================================================
    public ReservationResponse confirmerRemboursement(Integer reservationId) {

        ReservationVol res = reservationRepo.findById(reservationId)
                .orElseThrow(() -> new RuntimeException("Réservation introuvable"));

        if (res.getStatutReservation() != ReservationVol.StatutReservation.annulee)
            throw new RuntimeException(
                    "Cette réservation n'est pas annulée. " +
                            "Statut actuel : " + res.getStatutReservation()
            );

        if (res.getPaiement() == null)
            throw new RuntimeException("Aucun paiement associé à cette réservation");

        if (res.getPaiement().getStatut() == PaiementVol.StatutPaiement.rembourse)
            throw new RuntimeException("Ce remboursement est déjà confirmé");

        String paymentIntentId = res.getPaiement().getReferenceTx();

        if (paymentIntentId != null && paymentIntentId.startsWith("pi_")) {
            try {
                Refund refund = stripeService.rembourserPaiement(paymentIntentId, null);
                if (!"succeeded".equals(refund.getStatus())) {
                    throw new RuntimeException(
                            "Remboursement Stripe non confirmé. Statut : " + refund.getStatus());
                }
            } catch (StripeException e) {
                System.err.println("Stripe refund échoué (forcé manuellement) : "
                        + e.getMessage());
            }
        }

        res.getPaiement().setStatut(PaiementVol.StatutPaiement.rembourse);
        res.getPaiement().setDatePaiement(LocalDateTime.now());
        paiementRepo.save(res.getPaiement());

        return toResponse(res);
    }

    // ============================================================
    //  SOCIÉTÉ : TOUTES LES ANNULATIONS
    // ============================================================
    public List<ReservationResponse> toutesLesAnnulations() {
        return reservationRepo
                .findByStatutReservation(ReservationVol.StatutReservation.annulee)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    // ============================================================
    //  MAPPER
    // ============================================================
    private ReservationResponse toResponse(ReservationVol r) {
        PaiementVol.StatutPaiement statut = (r.getPaiement() != null)
                ? r.getPaiement().getStatut()
                : PaiementVol.StatutPaiement.en_attente;

        return ReservationResponse.builder()
                .id(r.getId())
                .reference(r.getReference())
                .touristeEmail(r.getTouriste().getEmail())
                .volAller(volService.toResponse(r.getVolAller()))
                .volRetour(r.getVolRetour() != null
                        ? volService.toResponse(r.getVolRetour()) : null)
                .typeBillet(r.getTypeBillet())
                .nbPassagers(r.getNbPassagers())
                .prixTotal(r.getPrixTotal())
                .dateReservation(r.getDateReservation())
                .statutPaiement(statut)
                .statutReservation(r.getStatutReservation())
                .build();
    }
}