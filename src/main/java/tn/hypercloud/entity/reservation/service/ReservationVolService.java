package tn.hypercloud.entity.reservation.service;

import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tn.hypercloud.entity.reservation.dto.OffreResponse;
import tn.hypercloud.entity.reservation.dto.ReservationRequest;
import tn.hypercloud.entity.reservation.dto.ReservationResponse;
import tn.hypercloud.entity.reservation.dto.StripePaymentRequest;
import tn.hypercloud.entity.reservation.*;
import tn.hypercloud.entity.user.User;
import tn.hypercloud.repository.reservation.OffreRepository;
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
    private final StripeService stripeService;
    private final EmailService emailService;
    private final QrCodeService qrCodeService; // ✅ AJOUT
    private final OffreRepository offreRepo;

    // ============================================================
    //  CLIENT : CRÉER UNE RÉSERVATION
    // ============================================================
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

        BigDecimal prixInitial = volAller.getPrix()
                .multiply(BigDecimal.valueOf(req.getNbPassagers()));
        if (volRetour != null)
            prixInitial = prixInitial.add(volRetour.getPrix()
                    .multiply(BigDecimal.valueOf(req.getNbPassagers())));

        BigDecimal prix = prixInitial;

        String ref = "TUN" + UUID.randomUUID()
                .toString().substring(0, 6).toUpperCase();

        // On ne compte que les réservations DÉJÀ PAYÉES pour le bonus
        long totalReservations = reservationRepo.countPaidByTouristeId(touriste.getId().longValue());
        boolean applyBonus = (totalReservations > 0) && ((totalReservations + 1) % 10 == 0); 
        // Note: Si il a 9 résas payées, la 10ème est bonus? Ou après 10? 
        // User a dit "chaque 10 résas". Donc la 10ème, 20ème...
        // Si countPaid == 9, alors (9+1)%10 == 0 -> La 10ème est gratuite/réduite.
        
        BigDecimal remise = BigDecimal.ZERO;
        
        if (applyBonus) {
            remise = prix.multiply(new BigDecimal("0.10"));
            prix = prix.subtract(remise);
        }

        Offre offreAppliquee = null;
        Offre volOffre = volAller.getOffre();

        // 1. Priorité au code promo saisi par l'utilisateur
        if (req.getOffreCode() != null && !req.getOffreCode().isEmpty()) {
            offreAppliquee = offreRepo.findByCodeAndActifTrue(req.getOffreCode()).orElse(null);
        } 
        // 2. Sinon, on utilise l'offre directement liée au vol
        else if (volOffre != null && Boolean.TRUE.equals(volOffre.getActif())) {
            offreAppliquee = volOffre;
        }

        if (offreAppliquee != null) {
            LocalDateTime now = LocalDateTime.now();
            if (now.isAfter(offreAppliquee.getDateDebut()) && now.isBefore(offreAppliquee.getDateFin())) {
                BigDecimal remiseOffre = prix.multiply(BigDecimal.valueOf(offreAppliquee.getPourcentage() / 100.0));
                prix = prix.subtract(remiseOffre);
            } else {
                offreAppliquee = null; // Expired or not yet started
            }
        }

        ReservationVol res = ReservationVol.builder()
                .touriste(touriste)
                .volAller(volAller)
                .volRetour(volRetour)
                .typeBillet(req.getTypeBillet())
                .nbPassagers(req.getNbPassagers())
                .prixTotal(prix)
                .prixInitial(prixInitial) // ✅ AJOUT
                .bonusApplique(applyBonus)
                .remiseBonus(remise)
                .offre(offreAppliquee)
                .reference(ref)
                .build();

        return toResponse(reservationRepo.save(res));
    }

    // ============================================================
    //  CLIENT : MES RÉSERVATIONS
    // ============================================================
    public List<ReservationResponse> mesReservations(String email) {
        User touriste = userRepo.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Utilisateur introuvable"));
        return reservationRepo.findByTouristeId(touriste.getId())
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    // ============================================================
    //  CLIENT : ANNULER (suppression simple sans remboursement Stripe)
    // ============================================================
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

    // ============================================================
    //  CLIENT : PAYER AVEC STRIPE
    // ============================================================
    public ReservationResponse payer(String email, StripePaymentRequest req) {

        ReservationVol res = reservationRepo.findById(req.getReservationId())
                .orElseThrow(() -> new RuntimeException("Réservation introuvable"));

        if (!res.getTouriste().getEmail().equals(email))
            throw new RuntimeException("Accès refusé");

        if (res.getPaiement() != null &&
                res.getPaiement().getStatut() == PaiementVol.StatutPaiement.paye)
            throw new RuntimeException("Réservation déjà payée");

        // -------------------------------------------------------
        //  APPEL STRIPE RÉEL
        // -------------------------------------------------------
        PaymentIntent intent;
        try {
            intent = stripeService.creerEtConfirmerPaiement(
                    res.getPrixTotal(),
                    req.getPaymentMethodId()
            );
        } catch (StripeException e) {
            PaiementVol paiementEchec = PaiementVol.builder()
                    .reservation(res)
                    .methode(req.getMethode())
                    .montant(res.getPrixTotal())
                    .referenceTx("STRIPE-FAIL-" + System.currentTimeMillis())
                    .statut(PaiementVol.StatutPaiement.echec)
                    .datePaiement(LocalDateTime.now())
                    .build();
            paiementRepo.save(paiementEchec);
            throw new RuntimeException("Paiement Stripe échoué : " + e.getMessage());
        }

        boolean paiementReussi = "succeeded".equals(intent.getStatus());

        PaiementVol.StatutPaiement statut = paiementReussi
                ? PaiementVol.StatutPaiement.paye
                : PaiementVol.StatutPaiement.echec;

        PaiementVol paiement = PaiementVol.builder()
                .reservation(res)
                .methode(req.getMethode())
                .montant(res.getPrixTotal())
                .referenceTx(intent.getId())
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

            // ✅ Sauvegarder la réservation
            ReservationVol resSauvegardee = reservationRepo.save(res);

            // ✅ Générer le QR code et le persister en base
            QrCodeVol qrCode = qrCodeService.genererEtSauvegarder(resSauvegardee);

            // ✅ Envoyer email avec QR code intégré
            emailService.envoyerEmailPaiement(resSauvegardee, qrCode.getImageBase64());

            return toResponse(resSauvegardee);

        } else {
            reservationRepo.delete(res);
            throw new RuntimeException("Paiement échoué, réservation annulée");
        }
    }

    // ============================================================
    //  SOCIÉTÉ : TOUTES LES RÉSERVATIONS
    // ============================================================
    public List<ReservationResponse> toutesLesReservations() {
        return reservationRepo.findAll()
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    // ============================================================
    //  SOCIÉTÉ : MODIFIER STATUT PAIEMENT
    // ============================================================
    public ReservationResponse modifierStatut(Integer reservationId, String nouveauStatut) {

        ReservationVol res = reservationRepo.findById(reservationId)
                .orElseThrow(() -> new RuntimeException("Réservation introuvable"));

        if (res.getPaiement() == null)
            throw new RuntimeException("Aucun paiement associé à cette réservation");

        try {
            PaiementVol.StatutPaiement statut =
                    PaiementVol.StatutPaiement.valueOf(nouveauStatut);
            res.getPaiement().setStatut(statut);
            paiementRepo.save(res.getPaiement());
        } catch (IllegalArgumentException e) {
            throw new RuntimeException(
                    "Statut invalide. Valeurs acceptées : en_attente, paye, echec");
        }

        return toResponse(reservationRepo.save(res));
    }

    // ============================================================
    //  SOCIÉTÉ : SUPPRIMER UNE RÉSERVATION (restitue les places)
    // ============================================================
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
    //  MAPPER
    // ============================================================
    ReservationResponse toResponse(ReservationVol r) {
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
                .prixInitial(r.getPrixInitial()) // ✅ AJOUT
                .dateReservation(r.getDateReservation())
                .statutPaiement(statut)
                .statutReservation(r.getStatutReservation())
                .bonusApplique(Boolean.TRUE.equals(r.getBonusApplique()))
                .remiseBonus(r.getRemiseBonus())
                .offre(r.getOffre() != null ? OffreResponse.builder()
                        .id(r.getOffre().getId())
                        .code(r.getOffre().getCode())
                        .pourcentage(r.getOffre().getPourcentage())
                        .build() : null)
                .build();
    }
}