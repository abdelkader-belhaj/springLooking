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

        BigDecimal prixAllerInitial = volAller.getPrix().multiply(BigDecimal.valueOf(req.getNbPassagers()));
        BigDecimal prixAllerFinal = prixAllerInitial;
        
        Offre offreAller = null;
        if (req.getOffreCode() != null && !req.getOffreCode().isEmpty()) {
            offreAller = offreRepo.findByCodeAndActifTrue(req.getOffreCode()).orElse(null);
        } else if (volAller.getOffre() != null && Boolean.TRUE.equals(volAller.getOffre().getActif())) {
            offreAller = volAller.getOffre();
        }

        if (offreAller != null) {
            LocalDateTime now = LocalDateTime.now();
            if (now.isAfter(offreAller.getDateDebut()) && now.isBefore(offreAller.getDateFin())) {
                BigDecimal remiseOffre = prixAllerInitial.multiply(BigDecimal.valueOf(offreAller.getPourcentage() / 100.0));
                prixAllerFinal = prixAllerInitial.subtract(remiseOffre);
            } else {
                offreAller = null;
            }
        }

        BigDecimal prixRetourInitial = BigDecimal.ZERO;
        BigDecimal prixRetourFinal = BigDecimal.ZERO;
        
        if (volRetour != null) {
            prixRetourInitial = volRetour.getPrix().multiply(BigDecimal.valueOf(req.getNbPassagers()));
            prixRetourFinal = prixRetourInitial;
            
            Offre offreRetour = volRetour.getOffre();
            if (offreRetour != null && Boolean.TRUE.equals(offreRetour.getActif())) {
                LocalDateTime now = LocalDateTime.now();
                if (now.isAfter(offreRetour.getDateDebut()) && now.isBefore(offreRetour.getDateFin())) {
                    BigDecimal remiseOffre = prixRetourInitial.multiply(BigDecimal.valueOf(offreRetour.getPourcentage() / 100.0));
                    prixRetourFinal = prixRetourInitial.subtract(remiseOffre);
                }
            }
        }

        BigDecimal prixInitial = prixAllerInitial.add(prixRetourInitial);
        BigDecimal prix = prixAllerFinal.add(prixRetourFinal);

        // On ne compte que les réservations DÉJÀ PAYÉES pour le bonus
        long totalReservations = reservationRepo.countPaidByTouristeId(touriste.getId().longValue());
        boolean applyBonus = (totalReservations > 0) && ((totalReservations + 1) % 3 == 0); 
        
        BigDecimal remise = BigDecimal.ZERO;
        if (applyBonus) {
            remise = prix.multiply(new BigDecimal("0.10"));
            prix = prix.subtract(remise);
        }

        Offre offreAppliquee = offreAller; // Save outbound offer for reference

        String ref = "TUN" + UUID.randomUUID()
                .toString().substring(0, 6).toUpperCase();

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

        res.setStatutReservation(ReservationVol.StatutReservation.archivee);
        reservationRepo.save(res);
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
    public ReservationResponse supprimerReservation(Integer reservationId) {
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

        res.setStatutReservation(ReservationVol.StatutReservation.archivee);
        return toResponse(reservationRepo.save(res));
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