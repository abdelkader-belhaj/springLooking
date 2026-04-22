package tn.hypercloud.service.accommodation;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import tn.hypercloud.entity.accommodation.Categorie;
import tn.hypercloud.entity.accommodation.Logement;
import tn.hypercloud.entity.accommodation.ReservationLogement;
import tn.hypercloud.entity.user.Role;
import tn.hypercloud.entity.user.User;
import tn.hypercloud.entity.user.Notification;

import tn.hypercloud.payload.request.LogementRequest;
import tn.hypercloud.payload.response.LogementResponse;

import tn.hypercloud.repository.accommodation.CategorieRepository;
import tn.hypercloud.repository.accommodation.LogementRepository;
import tn.hypercloud.repository.accommodation.ReservationLogementRepository;
import tn.hypercloud.repository.user.NotificationRepository;
import tn.hypercloud.repository.user.UserRepository;

import jakarta.transaction.Transactional;
import java.util.List;
import java.util.Comparator;
import java.time.LocalDate;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LogementService {

    private final LogementRepository logRepo;
    private final CategorieRepository catRepo;
    private final UserRepository userRepo;
    private final ReservationLogementRepository reservationRepo;
    private final NotificationRepository notificationRepo;

    // CREATE LOGEMENT
    public LogementResponse create(LogementRequest req) {

        User currentUser = getCurrentUser();

        Categorie categorie = catRepo.findById(req.getIdCategorie())
                .orElseThrow(() ->
                        new RuntimeException(
                                "Catégorie introuvable : " + req.getIdCategorie()));

        Logement logement = Logement.builder()
                .categorie(categorie)
                .hebergeur(currentUser)
                .nom(req.getNom())
                .description(req.getDescription())
                .imageUrl(req.getImageUrl())
                .videoUrl(req.getVideoUrl())
                .adresse(req.getAdresse())
                .ville(req.getVille())
                .prixNuit(req.getPrixNuit())
                .capacite(req.getCapacite())
                .disponible(req.isDisponible())
                .latitude(req.getLatitude())
                .longitude(req.getLongitude())
                .build();

        // Gérer les images multiples
        if (req.getImageUrls() != null && !req.getImageUrls().isEmpty()) {
            logement.setImageUrls(req.getImageUrls());
            // Définir la première image comme imageUrl principale
            logement.setImageUrl(req.getImageUrls().get(0));
        }

        Logement saved = logRepo.save(logement);

        // Notify all tourist clients about newly published logement.
        List<User> touristClients = userRepo.findByRole(Role.CLIENT_TOURISTE);
        if (!touristClients.isEmpty()) {
            List<Notification> notifications = touristClients.stream()
                .map(client -> Notification.builder()
                    .user(client)
                    .message("Nouveau logement disponible: " + saved.getNom() + " à " + saved.getVille() + ". Découvrez-le maintenant.")
                    .isRead(false)
                    .build())
                .collect(Collectors.toList());
            notificationRepo.saveAll(notifications);
        }

        return toResponse(saved);
    }

    // GET ALL LOGEMENTS
    public List<LogementResponse> getAll() {

        User currentUser = getCurrentUser();

        // ADMIN → voir tous les logements
        if (currentUser.getRole() == Role.ADMIN) {
            return logRepo.findAll()
                    .stream()
                    .map(this::toResponse)
                    .collect(Collectors.toList());
        }

        // HEBERGEUR → voir ses logements
        if (currentUser.getRole() == Role.HEBERGEUR) {
            return logRepo.findByHebergeurId(currentUser.getId())
                    .stream()
                    .map(this::toResponse)
                    .collect(Collectors.toList());
        }

        // USER → voir seulement logements disponibles
        return logRepo.findByDisponibleTrue()
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    // GET ALL PUBLIC LOGEMENTS — sans filtre d’utilisateur
    public List<LogementResponse> getAllPublic() {
        return logRepo.findAll()
                .stream()
                .filter(logement -> logement.getCategorie() != null && logement.getCategorie().isStatut())
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    // GET LOGEMENT BY ID
    public LogementResponse getById(Integer id) {
        Logement logement = findOrThrow(id);
        User currentUser = getCurrentUserOrNull();

        if ((currentUser == null || currentUser.getRole() == Role.CLIENT_TOURISTE)
                && !logement.isDisponible()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Ce logement est en maintenance. Coming soon.");
        }

        if ((currentUser == null || currentUser.getRole() == Role.CLIENT_TOURISTE)
                && (logement.getCategorie() == null || !logement.getCategorie().isStatut())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Cette catégorie est en maintenance. Coming soon.");
        }

        return toResponse(logement);
    }

    // GET LOGEMENTS BY CATEGORIE
    public List<LogementResponse> getByCategorie(Integer idCategorie) {
        Categorie categorie = catRepo.findById(idCategorie)
                .orElseThrow(() -> new RuntimeException("Catégorie introuvable : " + idCategorie));
        User currentUser = getCurrentUserOrNull();

        if (!categorie.isStatut() && (currentUser == null || currentUser.getRole() == Role.CLIENT_TOURISTE)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Cette catégorie est en maintenance. Coming soon.");
        }

        return logRepo.findByCategorieIdCategorie(idCategorie)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    // UPDATE LOGEMENT
    public LogementResponse update(Integer id, LogementRequest req) {

        User currentUser = getCurrentUser();
        Logement logement = findOrThrow(id);

        if (currentUser.getRole() == Role.HEBERGEUR &&
                !logement.getHebergeur().getId().equals(currentUser.getId())) {

            throw new RuntimeException(
                    "Accès refusé : ce logement ne vous appartient pas");
        }

        Categorie categorie = catRepo.findById(req.getIdCategorie())
                .orElseThrow(() ->
                        new RuntimeException(
                                "Catégorie introuvable : " + req.getIdCategorie()));

        logement.setCategorie(categorie);
        logement.setNom(req.getNom());
        logement.setDescription(req.getDescription());
        logement.setVideoUrl(req.getVideoUrl());
        logement.setAdresse(req.getAdresse());
        logement.setVille(req.getVille());
        logement.setPrixNuit(req.getPrixNuit());
        logement.setCapacite(req.getCapacite());
        logement.setDisponible(req.isDisponible());
        logement.setLatitude(req.getLatitude());
        logement.setLongitude(req.getLongitude());

        // Gérer les images multiples
        if (req.getImageUrls() != null && !req.getImageUrls().isEmpty()) {
            logement.setImageUrls(req.getImageUrls());
            // Définir la première image comme imageUrl principale
            logement.setImageUrl(req.getImageUrls().get(0));
        } else if (req.getImageUrl() != null && !req.getImageUrl().isEmpty()) {
            // Fallback pour rétro-compatibilité
            logement.setImageUrl(req.getImageUrl());
        }

        return toResponse(logRepo.save(logement));
    }

    // DELETE LOGEMENT
    @Transactional
    public void delete(Integer id) {

        User currentUser = getCurrentUser();
        Logement logement = findOrThrow(id);

        if (currentUser.getRole() == Role.HEBERGEUR &&
                !logement.getHebergeur().getId().equals(currentUser.getId())) {

            throw new RuntimeException(
                    "Accès refusé : ce logement ne vous appartient pas");
        }

        // Supprimer toutes les réservations liées avant de supprimer le logement
        List<ReservationLogement> reservations = reservationRepo.findByLogementIdLogement(id);
        if (!reservations.isEmpty()) {
            reservationRepo.deleteAll(reservations);
        }

        logRepo.delete(logement);
    }

    // GET CURRENT USER
    private User getCurrentUser() {

        String email = SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getName();

        return userRepo.findByEmail(email)
                .orElseThrow(() ->
                        new RuntimeException("Utilisateur introuvable"));
    }

    private User getCurrentUserOrNull() {
        if (SecurityContextHolder.getContext().getAuthentication() == null
                || SecurityContextHolder.getContext().getAuthentication() instanceof AnonymousAuthenticationToken) {
            return null;
        }

        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepo.findByEmail(email).orElse(null);
    }

    // FIND LOGEMENT
    private Logement findOrThrow(Integer id) {

        return logRepo.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("Logement introuvable : " + id));
    }

    // ENTITY → RESPONSE
    public LogementResponse toResponse(Logement logement) {
        LocalDate today = LocalDate.now();

        List<ReservationLogement> activeReservations = reservationRepo.findByLogementIdLogement(logement.getIdLogement())
            .stream()
            .filter(reservation -> reservation.getStatut() == ReservationLogement.StatutReservation.confirmee)
            .filter(reservation -> !reservation.getDateDebut().isAfter(today) && reservation.getDateFin().isAfter(today))
            .collect(Collectors.toList());

        int reservedPlacesNow = activeReservations.stream()
            .mapToInt(ReservationLogement::getNbPersonnes)
            .sum();

        int availablePlaces = Math.max(logement.getCapacite() - reservedPlacesNow, 0);
        boolean saturated = availablePlaces <= 0;

        LocalDate nextAvailableDate = activeReservations.stream()
            .map(ReservationLogement::getDateFin)
            .min(Comparator.naturalOrder())
            .orElse(null);

        return LogementResponse.builder()
                .idLogement(logement.getIdLogement())
                .idCategorie(logement.getCategorie().getIdCategorie())
                .nomCategorie(logement.getCategorie().getNomCategorie())
                .idHebergeur(logement.getHebergeur().getId())
                .nomHebergeur(logement.getHebergeur().getUsername())
                .nom(logement.getNom())
                .description(logement.getDescription())
                .imageUrl(logement.getImageUrl())
                .imageUrls(logement.getImageUrls())  // Ajouter les images multiples
                .videoUrl(logement.getVideoUrl())
                .adresse(logement.getAdresse())
                .ville(logement.getVille())
                .prixNuit(logement.getPrixNuit())
                .capacite(logement.getCapacite())
                .availablePlaces(availablePlaces)
                .saturated(saturated)
                .nextAvailableDate(nextAvailableDate)
                .disponible(logement.isDisponible() && !saturated)
                .latitude(logement.getLatitude())
                .longitude(logement.getLongitude())
                .dateCreation(logement.getDateCreation())
                .build();
    }
}
