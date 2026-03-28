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

    // ---- CRÉER RÉSERVATION ----
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

        // Calcul prix total
        BigDecimal prix = volAller.getPrix().multiply(BigDecimal.valueOf(req.getNbPassagers()));
        if (volRetour != null)
            prix = prix.add(volRetour.getPrix().multiply(BigDecimal.valueOf(req.getNbPassagers())));

        // Décrémenter places
        volAller.setPlaces(volAller.getPlaces() - req.getNbPassagers());
        volRepo.save(volAller);
        if (volRetour != null) {
            volRetour.setPlaces(volRetour.getPlaces() - req.getNbPassagers());
            volRepo.save(volRetour);
        }

        // Référence unique
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

    // ---- MES RÉSERVATIONS ----
    public List<ReservationResponse> mesReservations(String email) {
        User touriste = userRepo.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Utilisateur introuvable"));
        return reservationRepo.findByTouristeId(touriste.getId())
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    // ---- ANNULER ----
    public void annuler(String email, Integer reservationId) {
        ReservationVol res = reservationRepo.findById(reservationId)
                .orElseThrow(() -> new RuntimeException("Réservation introuvable"));
        if (!res.getTouriste().getEmail().equals(email))
            throw new RuntimeException("Accès refusé");

        // Remettre les places
        Vol volAller = res.getVolAller();
        volAller.setPlaces(volAller.getPlaces() + res.getNbPassagers());
        volRepo.save(volAller);
        if (res.getVolRetour() != null) {
            Vol volRetour = res.getVolRetour();
            volRetour.setPlaces(volRetour.getPlaces() + res.getNbPassagers());
            volRepo.save(volRetour);
        }

        reservationRepo.delete(res);
    }

    // ---- PAIEMENT STATIQUE (à remplacer par Flouci/Stripe) ----
    public ReservationResponse payer(String email, PaiementRequest req) {
        ReservationVol res = reservationRepo.findById(req.getReservationId())
                .orElseThrow(() -> new RuntimeException("Réservation introuvable"));
        if (!res.getTouriste().getEmail().equals(email))
            throw new RuntimeException("Accès refusé");

        PaiementVol paiement = PaiementVol.builder()
                .reservation(res)
                .methode(req.getMethode())
                .montant(res.getPrixTotal())
                .referenceTx("TX-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                .statut(PaiementVol.StatutPaiement.paye)   // statique → toujours succès
                .datePaiement(LocalDateTime.now())
                .build();

        paiementRepo.save(paiement);
        res.setPaiement(paiement);
        return toResponse(reservationRepo.save(res));
    }

    // ---- MAPPER ----
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
                .build();
    }
}