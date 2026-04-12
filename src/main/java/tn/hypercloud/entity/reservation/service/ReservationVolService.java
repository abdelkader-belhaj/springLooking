package tn.hypercloud.entity.reservation.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tn.hypercloud.entity.reservation.dto.PaiementRequest;
import tn.hypercloud.entity.reservation.dto.ReservationRequest;
import tn.hypercloud.entity.reservation.dto.ReservationResponse;
import tn.hypercloud.entity.reservation.*;
import tn.hypercloud.entity.user.User;
import tn.hypercloud.repository.reservation.PaiementVolRepository;
import tn.hypercloud.repository.reservation.ReservationVolRepository;
import tn.hypercloud.repository.reservation.VolRepository;
import tn.hypercloud.repository.user.UserRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReservationVolService {

    private final ReservationVolRepository reservationRepo;
    private final VolRepository volRepo;
    private final UserRepository userRepo;
    private final PaiementVolRepository paiementRepo;
    private final VolService volService;

    public ReservationResponse creer(String email, ReservationRequest req) {
        User touriste = userRepo.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Utilisateur introuvable"));

        Vol volAller = volRepo.findById(req.getVolAllerId())
                .orElseThrow(() -> new RuntimeException("Vol aller introuvable"));

        if (volAller.getPlaces() < req.getNbPassagers())
            throw new RuntimeException("Places insuffisantes sur le vol aller");

        Vol volRetour = null;
        if (req.getVolRetourId() != null) {
            volRetour = volRepo.findById(req.getVolRetourId())
                    .orElseThrow(() -> new RuntimeException("Vol retour introuvable"));
            if (volRetour.getPlaces() < req.getNbPassagers())
                throw new RuntimeException("Places insuffisantes sur le vol retour");
        }

        BigDecimal prix = volAller.getPrix().multiply(BigDecimal.valueOf(req.getNbPassagers()));
        if (volRetour != null)
            prix = prix.add(volRetour.getPrix().multiply(BigDecimal.valueOf(req.getNbPassagers())));

        String ref = "TUN" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();

        ReservationVol res = ReservationVol.builder()
                .touriste(touriste)
                .volAller(volAller)
                .volRetour(volRetour)
                .typeBillet(req.getTypeBillet())
                .nbPassagers(req.getNbPassagers())
                .prixTotal(prix)
                .reference(ref)
                .build();

        return toResponse(reservationRepo.save(res));
    }

    public List<ReservationResponse> mesReservations(String email) {
        User touriste = userRepo.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Utilisateur introuvable"));
        return reservationRepo.findByTouristeId(touriste.getId())
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    public void annuler(String email, Integer reservationId) {
        ReservationVol res = reservationRepo.findById(reservationId)
                .orElseThrow(() -> new RuntimeException("Réservation introuvable"));

        if (!res.getTouriste().getEmail().equals(email))
            throw new RuntimeException("Accès refusé");

        boolean estPayee = res.getPaiement() != null &&
                res.getPaiement().getStatut() == PaiementVol.StatutPaiement.paye;

        if (estPayee) {
            Vol volAller = res.getVolAller();
            volAller.setPlaces(volAller.getPlaces() + res.getNbPassagers());
            volRepo.save(volAller);

            if (res.getVolRetour() != null) {
                Vol volRetour = res.getVolRetour();
                volRetour.setPlaces(volRetour.getPlaces() + res.getNbPassagers());
                volRepo.save(volRetour);
            }
        }

        reservationRepo.delete(res);
    }

    public ReservationResponse payer(String email, PaiementRequest req) {
        ReservationVol res = reservationRepo.findById(req.getReservationId())
                .orElseThrow(() -> new RuntimeException("Réservation introuvable"));

        if (!res.getTouriste().getEmail().equals(email))
            throw new RuntimeException("Accès refusé");

        if (res.getPaiement() != null &&
                res.getPaiement().getStatut() == PaiementVol.StatutPaiement.paye)
            throw new RuntimeException("Réservation déjà payée");

        boolean paiementReussi = true;
        String referenceTx = "TX-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        PaiementVol.StatutPaiement statut = paiementReussi
                ? PaiementVol.StatutPaiement.paye
                : PaiementVol.StatutPaiement.echec;

        PaiementVol paiement = PaiementVol.builder()
                .reservation(res)
                .methode(req.getMethode())
                .montant(res.getPrixTotal())
                .referenceTx(referenceTx)
                .statut(statut)
                .datePaiement(LocalDateTime.now())
                .build();

        paiementRepo.save(paiement);
        res.setPaiement(paiement);

        if (paiementReussi) {
            Vol volAller = res.getVolAller();
            if (volAller.getPlaces() < res.getNbPassagers())
                throw new RuntimeException("Plus de places disponibles sur le vol aller");

            volAller.setPlaces(volAller.getPlaces() - res.getNbPassagers());
            volRepo.save(volAller);

            if (res.getVolRetour() != null) {
                Vol volRetour = res.getVolRetour();
                if (volRetour.getPlaces() < res.getNbPassagers())
                    throw new RuntimeException("Plus de places disponibles sur le vol retour");
                volRetour.setPlaces(volRetour.getPlaces() - res.getNbPassagers());
                volRepo.save(volRetour);
            }

            return toResponse(reservationRepo.save(res));

        } else {
            reservationRepo.delete(res);
            throw new RuntimeException("Paiement échoué, réservation annulée");
        }
    }

    public List<ReservationResponse> toutesLesReservations() {
        return reservationRepo.findAll()
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    public ReservationResponse modifierStatut(Integer reservationId, String nouveauStatut) {
        ReservationVol res = reservationRepo.findById(reservationId)
                .orElseThrow(() -> new RuntimeException("Réservation introuvable"));

        if (res.getPaiement() == null)
            throw new RuntimeException("Aucun paiement associé à cette réservation");

        try {
            PaiementVol.StatutPaiement statut = PaiementVol.StatutPaiement.valueOf(nouveauStatut);
            res.getPaiement().setStatut(statut);
            paiementRepo.save(res.getPaiement());
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Statut invalide. Valeurs acceptées : en_attente, paye, echec");
        }

        return toResponse(reservationRepo.save(res));
    }

    public void supprimerReservation(Integer reservationId) {
        ReservationVol res = reservationRepo.findById(reservationId)
                .orElseThrow(() -> new RuntimeException("Réservation introuvable"));

        boolean estPayee = res.getPaiement() != null &&
                res.getPaiement().getStatut() == PaiementVol.StatutPaiement.paye;

        if (estPayee) {
            Vol volAller = res.getVolAller();
            volAller.setPlaces(volAller.getPlaces() + res.getNbPassagers());
            volRepo.save(volAller);

            if (res.getVolRetour() != null) {
                Vol volRetour = res.getVolRetour();
                volRetour.setPlaces(volRetour.getPlaces() + res.getNbPassagers());
                volRepo.save(volRetour);
            }
        }

        reservationRepo.delete(res);
    }

    // ============================================================
    //  MAPPER — ✅ statutReservation ajouté
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
                .volRetour(r.getVolRetour() != null ? volService.toResponse(r.getVolRetour()) : null)
                .typeBillet(r.getTypeBillet())
                .nbPassagers(r.getNbPassagers())
                .prixTotal(r.getPrixTotal())
                .dateReservation(r.getDateReservation())
                .statutPaiement(statut)
                .statutReservation(r.getStatutReservation()) // ✅ correction
                .build();
    }
}