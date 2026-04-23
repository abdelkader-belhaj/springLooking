package tn.hypercloud.entity.reservation.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.hypercloud.entity.reservation.ReclamationVol;
import tn.hypercloud.entity.reservation.ReservationVol;
import tn.hypercloud.entity.reservation.dto.ReclamationCreateRequest;
import tn.hypercloud.entity.reservation.dto.ReclamationReplyRequest;
import tn.hypercloud.entity.reservation.dto.ReclamationResponse;
import tn.hypercloud.entity.reservation.dto.ReclamationUpdateRequest;
import tn.hypercloud.repository.reservation.ReclamationVolRepository;
import tn.hypercloud.repository.reservation.ReservationVolRepository;
import tn.hypercloud.repository.user.UserRepository;
import tn.hypercloud.entity.user.User;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReclamationVolService {

    private final ReclamationVolRepository reclamationRepo;
    private final UserRepository userRepo;
    private final ReservationVolRepository reservationRepo;
    private final EmailService emailService;

    @Transactional
    public ReclamationResponse creer(String email, ReclamationCreateRequest req) {
        User touriste = userRepo.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Utilisateur introuvable"));

        ReservationVol reservation = reservationRepo.findById(req.getReservationId())
                .orElseThrow(() -> new RuntimeException("Réservation introuvable"));
        if (!reservation.getTouriste().getId().equals(touriste.getId())) {
            throw new RuntimeException("Accès refusé");
        }

        ReclamationVol r = ReclamationVol.builder()
                .touriste(touriste)
                .reservation(reservation)
                .priorite(ReclamationVol.Priorite.valueOf(req.getPriorite().name()))
                .sujet(req.getSujet())
                .statut(ReclamationVol.Statut.ouverte)
                .clientLu(false)
                .build();

        return toResponse(reclamationRepo.save(r));
    }

    @Transactional(readOnly = true)
    public List<ReclamationResponse> mesReclamations(String email) {
        System.out.println("DEBUG: Recherche réclamations pour email: " + email);
        User touriste = userRepo.findByEmail(email)
                .orElse(null);
        
        if (touriste == null) {
            System.out.println("DEBUG: Utilisateur non trouvé pour email: " + email);
            // Retourner liste vide au lieu de lancer une exception
            return new ArrayList<>();
        }
        
        System.out.println("DEBUG: Utilisateur trouvé: " + touriste.getId() + " - " + touriste.getEmail());
        List<ReclamationVol> reclamations = reclamationRepo.findByTouristeIdOrderByDateCreationDesc(touriste.getId());
        System.out.println("DEBUG: Nombre de réclamations trouvées: " + reclamations.size());
        
        return reclamations.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public long unreadCount(String email) {
        User touriste = userRepo.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Utilisateur introuvable"));
        return reclamationRepo.countUnreadRepliesForTouriste(touriste.getId());
    }

    @Transactional
    public void marquerLu(String email, Integer reclamationId) {
        User touriste = userRepo.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Utilisateur introuvable"));
        ReclamationVol r = reclamationRepo.findById(reclamationId)
                .orElseThrow(() -> new RuntimeException("Réclamation introuvable"));
        if (!r.getTouriste().getId().equals(touriste.getId())) {
            throw new RuntimeException("Accès refusé");
        }
        r.setClientLu(true);
        reclamationRepo.save(r);
    }

    @Transactional
    public ReclamationResponse modifier(String email, Integer reclamationId, ReclamationUpdateRequest req) {
        User touriste = userRepo.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Utilisateur introuvable"));
        ReclamationVol r = reclamationRepo.findById(reclamationId)
                .orElseThrow(() -> new RuntimeException("Réclamation introuvable"));
        if (!r.getTouriste().getId().equals(touriste.getId())) {
            throw new RuntimeException("Accès refusé");
        }
        if (r.getStatut() != ReclamationVol.Statut.ouverte) {
            throw new RuntimeException("Impossible de modifier une réclamation déjà répondue");
        }

        r.setPriorite(ReclamationVol.Priorite.valueOf(req.getPriorite().name()));
        r.setSujet(req.getSujet());
        return toResponse(reclamationRepo.save(r));
    }

    @Transactional(readOnly = true)
    public List<ReclamationResponse> toutesPourSociete(String societeEmail) {
        User societe = userRepo.findByEmail(societeEmail)
                .orElseThrow(() -> new RuntimeException("Utilisateur introuvable"));
        return reclamationRepo.findForSociete(societe.getId())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public ReclamationResponse repondre(String societeEmail, Integer reclamationId, ReclamationReplyRequest req) {
        User societe = userRepo.findByEmail(societeEmail)
                .orElseThrow(() -> new RuntimeException("Utilisateur introuvable"));

        ReclamationVol r = reclamationRepo.findByIdWithReservationVolUsers(reclamationId)
                .orElseThrow(() -> new RuntimeException("Réclamation introuvable"));

        Long societeId = societe.getId();
        Long volAllerSocieteId = r.getReservation().getVolAller().getUser().getId();
        Long volRetourSocieteId = (r.getReservation().getVolRetour() != null)
                ? r.getReservation().getVolRetour().getUser().getId()
                : null;
        boolean owned = societeId != null && (societeId.equals(volAllerSocieteId) || societeId.equals(volRetourSocieteId));
        if (!owned) {
            throw new RuntimeException("Accès refusé");
        }

        r.setReponse(req.getReponse());
        r.setDateReponse(LocalDateTime.now());
        r.setStatut(ReclamationVol.Statut.repondue);
        r.setClientLu(false); // déclenche notif "in-site"

        ReclamationVol saved = reclamationRepo.save(r);
        emailService.envoyerEmailReponseReclamation(saved);
        return toResponse(saved);
    }

    private ReclamationResponse toResponse(ReclamationVol r) {
        return ReclamationResponse.builder()
                .id(r.getId())
                .reservationId(r.getReservation().getId())
                .reservationReference(r.getReservation().getReference())
                .touristeEmail(r.getTouriste() != null ? r.getTouriste().getEmail() : null)
                .priorite(r.getPriorite() != null ? r.getPriorite().name() : null)
                .statut(r.getStatut() != null ? r.getStatut().name() : null)
                .sujet(r.getSujet())
                .reponse(r.getReponse())
                .dateCreation(r.getDateCreation())
                .dateReponse(r.getDateReponse())
                .clientLu(Boolean.TRUE.equals(r.getClientLu()))
                .build();
    }
}

