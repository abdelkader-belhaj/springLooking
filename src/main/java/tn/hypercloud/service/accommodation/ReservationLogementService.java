package tn.hypercloud.service.accommodation;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import tn.hypercloud.entity.accommodation.Logement;
import tn.hypercloud.entity.accommodation.ReservationLogement;
import tn.hypercloud.entity.accommodation.ReservationLogement.StatutReservation;
import tn.hypercloud.entity.user.Role;
import tn.hypercloud.entity.user.User;

import tn.hypercloud.payload.request.ReservationRequest;
import tn.hypercloud.payload.response.ReservationResponse;

import tn.hypercloud.entity.user.Notification;
import tn.hypercloud.repository.accommodation.LogementRepository;
import tn.hypercloud.repository.accommodation.ReservationLogementRepository;
import tn.hypercloud.repository.user.NotificationRepository;
import tn.hypercloud.repository.user.UserRepository;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;

import java.util.List;
import java.util.Comparator;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReservationLogementService {

    private final ReservationLogementRepository resRepo;
    private final LogementRepository logRepo;
    private final UserRepository userRepo;
    private final NotificationRepository notifRepo;
    private final EmailService emailService;

    // CREATE RESERVATION
    public ReservationResponse create(ReservationRequest req) {

        User currentUser = getCurrentUser();

        Logement logement = logRepo.findById(req.getIdLogement())
                .orElseThrow(() ->
                        new RuntimeException(
                                "Logement introuvable : " + req.getIdLogement()));

        if (!logement.isDisponible()) {
            throw new RuntimeException(
                    "Ce logement n'est pas disponible");
        }

        int availablePlacesNow = getAvailablePlacesNow(logement);
        if (availablePlacesNow <= 0) {
            LocalDate nextDate = getNextAvailableDate(logement);
            String nextDateMessage = nextDate != null
                ? "Une place sera disponible le " + nextDate + "."
                : "Aucune date de libération disponible pour le moment.";
            throw new RuntimeException("Ce logement est saturé pour le moment. " + nextDateMessage);
        }

        if (!req.getDateFin().isAfter(req.getDateDebut())) {
            throw new RuntimeException(
                    "La date de fin doit être après la date de début");
        }

        if (req.getNbPersonnes() < 1 || req.getNbPersonnes() > logement.getCapacite()) {
            throw new RuntimeException("Nombre de personnes invalide pour la capacité du logement");
        }

        validateDateOverlap(logement.getIdLogement(), req.getDateDebut(), req.getDateFin(), null, req.getNbPersonnes());

        long nbJours = ChronoUnit.DAYS.between(
                req.getDateDebut(),
                req.getDateFin());

        BigDecimal prixTotal = logement.getPrixNuit()
                .multiply(BigDecimal.valueOf(nbJours));

        if (req.getPrixFinalNegocie() != null) {
            prixTotal = req.getPrixFinalNegocie();
        }

        String smartLockCode = generateSmartLockCode();

        ReservationLogement reservation = ReservationLogement.builder()
                .logement(logement)
                .client(currentUser)
                .dateDebut(req.getDateDebut())
                .dateFin(req.getDateFin())
                .nbPersonnes(req.getNbPersonnes())
                .prixTotal(prixTotal)
                .statut(StatutReservation.confirmee)
                .archived(false)
            .dateReservation(nowUtc())
                .smartLockCode(smartLockCode)
                .build();

        ReservationLogement savedReservation = resRepo.save(reservation);

        emailService.sendReservationConfirmationEmail(
                savedReservation.getClient().getEmail(),
            savedReservation.getClient().getFullName(),
                savedReservation.getLogement().getNom(),
                savedReservation.getDateDebut(),
                savedReservation.getDateFin(),
                savedReservation.getPrixTotal()
        );

        Notification notif = Notification.builder()
            .user(logement.getHebergeur())
            .message("Une réservation confirmée a été effectuée par " + currentUser.getFullName() + " pour le logement : " + logement.getNom() + ".")
            .isRead(false)
            .build();
        notifRepo.save(notif);

        Notification clientSmartLockNotif = Notification.builder()
            .user(currentUser)
            .message("Réservation confirmée pour " + logement.getNom() + ". Votre Clé Intelligente est une clé secrète unique: ne la partagez avec personne. Ouvrez Mes Réservations pour l'activer.")
            .isRead(false)
            .build();
        notifRepo.save(clientSmartLockNotif);

        return toResponse(savedReservation);
    }

    // GET ALL RESERVATIONS
    public List<ReservationResponse> getAll() {

        User currentUser = getCurrentUser();

        // ADMIN → toutes les réservations
        if (currentUser.getRole() == Role.ADMIN) {

            return resRepo.findAll()
                    .stream()
                    .map(this::toResponse)
                    .collect(Collectors.toList());
        }

        // HEBERGEUR → réservations de ses logements
        if (currentUser.getRole() == Role.HEBERGEUR) {

            return resRepo.findByLogementHebergeurId(currentUser.getId())
                    .stream()
                    .map(this::toResponse)
                    .collect(Collectors.toList());
        }

        // CLIENT_TOURISTE → ses réservations
        return resRepo.findByClientId(currentUser.getId())
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    // GET RESERVATION BY ID
    public ReservationResponse getById(Integer id) {
        return toResponse(findOrThrow(id));
    }

    // ANNULER RESERVATION
    public ReservationResponse annuler(Integer id) {

        User currentUser = getCurrentUser();
        ReservationLogement reservation = findOrThrow(id);

        if (currentUser.getRole() == Role.CLIENT_TOURISTE &&
                !reservation.getClient().getId().equals(currentUser.getId())) {

            throw new RuntimeException(
                    "Accès refusé : ce n'est pas votre réservation");
        }
        
        if (currentUser.getRole() == Role.CLIENT_TOURISTE) {
            if (isWithinCancelOrModifyWindow(reservation) && isConfirmedLikeStatus(reservation.getStatut())) {
                resRepo.delete(reservation);
                reservation.setStatut(StatutReservation.annulee);
                return toResponse(reservation);
            } else {
                throw new RuntimeException("Délai de 2 heures dépassé ou statut non annulable.");
            }
        }

        reservation.setStatut(StatutReservation.annulee);
        ReservationLogement saved = resRepo.save(reservation);

        Notification notif = Notification.builder()
            .user(reservation.getClient())
            .message("Votre réservation pour le logement " + reservation.getLogement().getNom() + " a été annulée.")
            .isRead(false)
            .build();
        notifRepo.save(notif);

        return toResponse(saved);
    }
    
    // MODIFIER RESERVATION
    public ReservationResponse modifier(Integer id, ReservationRequest req) {
        User currentUser = getCurrentUser();
        ReservationLogement reservation = findOrThrow(id);

        if (!reservation.getClient().getId().equals(currentUser.getId())) {
            throw new RuntimeException("Accès refusé");
        }

        if (!isWithinCancelOrModifyWindow(reservation) || !isConfirmedLikeStatus(reservation.getStatut())) {
            throw new RuntimeException("Délai de modification dépassé ou statut incompatible");
        }

        if (!req.getDateFin().isAfter(req.getDateDebut())) {
            throw new RuntimeException("La date de fin doit être après la date de début");
        }

        if (req.getNbPersonnes() < 1 || req.getNbPersonnes() > reservation.getLogement().getCapacite()) {
            throw new RuntimeException("Nombre de personnes invalide pour la capacité du logement");
        }

        validateDateOverlap(
                reservation.getLogement().getIdLogement(),
                req.getDateDebut(),
                req.getDateFin(),
            reservation.getIdReservation(),
            req.getNbPersonnes()
        );

        reservation.setDateDebut(req.getDateDebut());
        reservation.setDateFin(req.getDateFin());
        reservation.setNbPersonnes(req.getNbPersonnes());
        reservation.setStatut(StatutReservation.confirmee);

        long nbJours = ChronoUnit.DAYS.between(req.getDateDebut(), req.getDateFin());
        BigDecimal nouveauPrix = reservation.getLogement().getPrixNuit().multiply(BigDecimal.valueOf(nbJours));

        // Si le prix négocié est transmis lors de la modification, on le préserve
        if (req.getPrixFinalNegocie() != null) {
            nouveauPrix = req.getPrixFinalNegocie();
        }
        
        reservation.setPrixTotal(nouveauPrix);

        ReservationLogement savedReservation = resRepo.save(reservation);

        Notification notif = Notification.builder()
            .user(reservation.getLogement().getHebergeur())
            .message("Le client " + currentUser.getFullName() + " a modifié sa réservation confirmée pour le logement : " + reservation.getLogement().getNom() + ".")
            .isRead(false)
            .build();
        notifRepo.save(notif);

        return toResponse(savedReservation);
    }

    // DELETE RESERVATION
    public void delete(Integer id) {
        ReservationLogement reservation = findOrThrow(id);
        User currentUser = getCurrentUser();
        
        if (currentUser.getRole() == Role.CLIENT_TOURISTE && !reservation.getClient().getId().equals(currentUser.getId())) {
            throw new RuntimeException("Accès refusé : ce n'est pas votre réservation");
        }
        resRepo.delete(reservation);
    }

    // ARCHIVE / UNARCHIVE RESERVATION
    @Transactional
    public ReservationResponse archive(Integer id) {
        return updateArchiveState(id, true);
    }

    @Transactional
    public ReservationResponse unarchive(Integer id) {
        return updateArchiveState(id, false);
    }

    private ReservationResponse updateArchiveState(Integer id, boolean archived) {
        User currentUser = getCurrentUser();
        ReservationLogement reservation = findOrThrow(id);

        if (currentUser.getRole() == Role.HEBERGEUR
                && !reservation.getLogement().getHebergeur().getId().equals(currentUser.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Accès refusé : cette réservation ne vous appartient pas");
        }

        if (currentUser.getRole() == Role.CLIENT_TOURISTE) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Action réservée aux administrateurs et hébergeurs");
        }

        reservation.setArchived(archived);
        ReservationLogement saved = resRepo.saveAndFlush(reservation);
        return toResponse(saved);
    }

    // GET CURRENT USER
    private User getCurrentUser() {

        String email = SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getName();

        return userRepo.findByEmail(email)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Utilisateur introuvable"));
    }

    // FIND RESERVATION
    private ReservationLogement findOrThrow(Integer id) {

        return resRepo.findById(id)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Réservation introuvable : " + id));
    }

    // ENTITY → RESPONSE
    private ReservationResponse toResponse(ReservationLogement reservation) {
        StatutReservation statut = normalizeStatus(reservation.getStatut());
        boolean canCancelOrModify = false;
        if (reservation.getDateReservation() != null && isConfirmedLikeStatus(statut)) {
            canCancelOrModify = isWithinCancelOrModifyWindow(reservation);
        }

        return ReservationResponse.builder()
                .idReservation(reservation.getIdReservation())
                .idLogement(reservation.getLogement().getIdLogement())
                .nomLogement(reservation.getLogement().getNom())
                .villeLogement(reservation.getLogement().getVille())
                .idClient(reservation.getClient().getId())
                .nomClient(reservation.getClient().getFullName())
                .dateDebut(reservation.getDateDebut())
                .dateFin(reservation.getDateFin())
                .nbPersonnes(reservation.getNbPersonnes())
                .prixTotal(reservation.getPrixTotal())
                .statut(statut.name())
                .dateReservation(reservation.getDateReservation())
                .canCancelOrModify(canCancelOrModify)
                .capaciteLogement(reservation.getLogement().getCapacite())
                .smartLockCode(reservation.getSmartLockCode())
                .archived(reservation.isArchived())
                .build();
    }

    private StatutReservation normalizeStatus(StatutReservation statut) {
        if (statut == null || statut == StatutReservation.en_attente) {
            return StatutReservation.confirmee;
        }
        return statut;
    }

    private boolean isWithinCancelOrModifyWindow(ReservationLogement reservation) {
        // Legacy rows may have null timestamps; keep them editable/cancelable instead of failing incorrectly.
        if (reservation.getDateReservation() == null) return true;
        LocalDateTime reservedAt = reservation.getDateReservation();

        long elapsedUtcMinutes = Duration.between(reservedAt, nowUtc()).toMinutes();
        long elapsedLocalMinutes = Duration.between(reservedAt, LocalDateTime.now()).toMinutes();

        long effectiveElapsedMinutes = getEffectiveElapsedMinutes(elapsedUtcMinutes, elapsedLocalMinutes);
        return effectiveElapsedMinutes <= 120;
    }

    private long getEffectiveElapsedMinutes(long elapsedUtcMinutes, long elapsedLocalMinutes) {
        boolean utcValid = elapsedUtcMinutes >= 0;
        boolean localValid = elapsedLocalMinutes >= 0;

        if (!utcValid && !localValid) {
            return 0;
        }
        if (utcValid && !localValid) {
            return elapsedUtcMinutes;
        }
        if (!utcValid) {
            return elapsedLocalMinutes;
        }

        return Math.min(elapsedUtcMinutes, elapsedLocalMinutes);
    }

    private boolean isConfirmedLikeStatus(StatutReservation statut) {
        return statut == StatutReservation.confirmee || statut == StatutReservation.en_attente;
    }

    private void validateDateOverlap(Integer logementId, LocalDate dateDebut, LocalDate dateFin, Integer excludeReservationId, int requestedPeople) {
        List<ReservationLogement> reservations = resRepo.findByLogementIdLogement(logementId);
        Logement logement = logRepo.findById(logementId)
                .orElseThrow(() -> new RuntimeException("Logement introuvable : " + logementId));

        int alreadyReserved = reservations.stream()
                .filter(r -> excludeReservationId == null || !r.getIdReservation().equals(excludeReservationId))
                .filter(r -> r.getStatut() == StatutReservation.confirmee || r.getStatut() == StatutReservation.en_attente)
                .filter(r -> dateDebut.isBefore(r.getDateFin()) && dateFin.isAfter(r.getDateDebut()))
                .mapToInt(ReservationLogement::getNbPersonnes)
                .sum();

        if (alreadyReserved + requestedPeople > logement.getCapacite()) {
            throw new RuntimeException("Capacité insuffisante pour cette période. Places restantes: " + Math.max(logement.getCapacite() - alreadyReserved, 0));
        }
    }

    private int getAvailablePlacesNow(Logement logement) {
        LocalDate today = LocalDate.now();
        int reservedNow = resRepo.findByLogementIdLogement(logement.getIdLogement())
                .stream()
                .filter(r -> r.getStatut() == StatutReservation.confirmee)
                .filter(r -> !r.getDateDebut().isAfter(today) && r.getDateFin().isAfter(today))
                .mapToInt(ReservationLogement::getNbPersonnes)
                .sum();
        return Math.max(logement.getCapacite() - reservedNow, 0);
    }

    private LocalDate getNextAvailableDate(Logement logement) {
        LocalDate today = LocalDate.now();
        return resRepo.findByLogementIdLogement(logement.getIdLogement())
                .stream()
                .filter(r -> r.getStatut() == StatutReservation.confirmee)
                .filter(r -> !r.getDateDebut().isAfter(today) && r.getDateFin().isAfter(today))
                .map(ReservationLogement::getDateFin)
                .min(Comparator.naturalOrder())
                .orElse(null);
    }

    private LocalDateTime nowUtc() {
        return LocalDateTime.now(ZoneOffset.UTC);
    }

    private String generateSmartLockCode() {
        return "LOCK-" + java.util.UUID.randomUUID().toString().substring(0, 6).toUpperCase();
    }
}