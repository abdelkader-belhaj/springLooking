package tn.hypercloud.entity.reservation.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tn.hypercloud.entity.reservation.*;
import tn.hypercloud.entity.reservation.dto.ReservationResponse;
import tn.hypercloud.repository.reservation.PaiementVolRepository;
import tn.hypercloud.repository.reservation.ReservationVolRepository;
import tn.hypercloud.repository.reservation.VolRepository;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AnnulationVolService {

    private final ReservationVolRepository reservationRepo;
    private final VolRepository volRepo;
    private final PaiementVolRepository paiementRepo;
    private final VolService volService;

    // ============================================================
    //  CLIENT : DEMANDER ANNULATION + REMBOURSEMENT
    //  Règles :
    //  1. Réservation doit être "active"
    //  2. Paiement doit être "paye"
    //  3. Départ doit être dans plus de 48h
    //  4. Restitue les places immédiatement
    //  5. Marque paiement → "rembourse"
    //  6. Marque réservation → "annulee"
    //  TODO : brancher Flouci remboursement ici
    // ============================================================
    public ReservationResponse demanderAnnulation(String email, Integer reservationId) {

        ReservationVol res = reservationRepo.findById(reservationId)
                .orElseThrow(() -> new RuntimeException("Réservation introuvable"));

        // Vérifier que c'est bien le client propriétaire
        if (!res.getTouriste().getEmail().equals(email))
            throw new RuntimeException("Accès refusé");

        // Vérifier que la réservation est encore active
        if (res.getStatutReservation() != ReservationVol.StatutReservation.active)
            throw new RuntimeException("Cette réservation est déjà annulée");

        // Vérifier que le paiement est bien "paye"
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
        //  RÈGLE 48H : vérifier date/heure du vol aller
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
        //  Restituer les places du vol aller
        // -------------------------------------------------------
        Vol volAller = res.getVolAller();
        volAller.setPlaces(volAller.getPlaces() + res.getNbPassagers());
        volRepo.save(volAller);

        // Restituer les places du vol retour si aller-retour
        if (res.getVolRetour() != null) {
            Vol volRetour = res.getVolRetour();
            volRetour.setPlaces(volRetour.getPlaces() + res.getNbPassagers());
            volRepo.save(volRetour);
        }

        // -------------------------------------------------------
        //  Mettre à jour le statut du paiement → rembourse
        //  TODO FLOUCI :
        //  FlouciResponse flouciRes = flouciClient.rembourser(
        //      res.getPaiement().getReferenceTx()
        //  );
        //  if (!flouciRes.isSuccess()) throw new RuntimeException("Remboursement Flouci échoué");
        // -------------------------------------------------------
        res.getPaiement().setStatut(PaiementVol.StatutPaiement.rembourse);
        res.getPaiement().setDatePaiement(LocalDateTime.now());
        paiementRepo.save(res.getPaiement());

        // Mettre à jour le statut de la réservation → annulee
        res.setStatutReservation(ReservationVol.StatutReservation.annulee);
        reservationRepo.save(res);

        return toResponse(res);
    }

    // ============================================================
    //  SOCIÉTÉ : CONFIRMER LE REMBOURSEMENT MANUELLEMENT
    //  Utile avant intégration Flouci ou pour forcer le statut
    //  PUT /api/annulations/{id}/confirmer-remboursement
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

        res.getPaiement().setStatut(PaiementVol.StatutPaiement.rembourse);
        res.getPaiement().setDatePaiement(LocalDateTime.now());
        paiementRepo.save(res.getPaiement());

        return toResponse(res);
    }

    // ============================================================
    //  SOCIÉTÉ : VOIR TOUTES LES RÉSERVATIONS ANNULÉES
    //  GET /api/annulations
    // ============================================================
    public java.util.List<ReservationResponse> toutesLesAnnulations() {
        return reservationRepo
                .findByStatutReservation(ReservationVol.StatutReservation.annulee)
                .stream()
                .map(this::toResponse)
                .collect(java.util.stream.Collectors.toList());
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
                        ? volService.toResponse(r.getVolRetour())
                        : null)
                .typeBillet(r.getTypeBillet())
                .nbPassagers(r.getNbPassagers())
                .prixTotal(r.getPrixTotal())
                .dateReservation(r.getDateReservation())
                .statutPaiement(statut)
                .statutReservation(r.getStatutReservation())
                .build();
    }
}