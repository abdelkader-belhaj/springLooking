package tn.hypercloud.service.transport;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.hypercloud.controller.transport.RealTimeTransportController;
import tn.hypercloud.dto.transport.DriverNotificationDTO;
import tn.hypercloud.dto.transport.MatchingDriverCardDTO;
import tn.hypercloud.entity.transport.*;
import tn.hypercloud.entity.transport.enums.*;
import tn.hypercloud.repository.transport.*;

import java.time.LocalDateTime;
import java.util.List;

@Service
@AllArgsConstructor
public class MatchingServiceImpl implements IMatchingService {

    private final MatchingRepository matchingRepository;
    private final DemandeCoursRepository demandeCoursRepository;
    private final CourseRepository courseRepository;
    private final VehiculeRepository vehiculeRepository;
    private final ChauffeurRepository chauffeurRepository;
    private final ICourseService courseService;
    private final RealTimeTransportController realTimeController;
    // ====================== CRUD ======================
    @Override
    public Matching addMatching(Matching matching) {
        return matchingRepository.save(matching);
    }

    @Override
    public Matching updateMatching(Matching matching) {
        return matchingRepository.save(matching);
    }

    @Override
    public void deleteMatching(Long id) {
        matchingRepository.deleteById(id);
    }

    @Override
    public Matching getMatchingById(Long id) {
        return matchingRepository.findById(id).orElse(null);
    }

    @Override
    public List<Matching> getAllMatchings() {
        return matchingRepository.findAll();
    }

    @Override
    public List<Matching> getMatchingsByChauffeur(Chauffeur chauffeur) {
        return matchingRepository.findByChauffeur(chauffeur);
    }

    @Override
    public List<Matching> getMatchingsByStatut(MatchingStatut statut) {
        return matchingRepository.findByStatut(statut);
    }

    // ====================== LOGIQUE MÉTIER CRITIQUE (PDF page 6) ======================
    @Override
    @Transactional
    public Matching acceptMatching(Long matchingId) {
        Matching matching = getMatchingById(matchingId);
        if (matching == null) {
            throw new RuntimeException("Matching non trouvé");
        }

        DemandeCourse demande = matching.getDemande();
        Chauffeur chauffeur = matching.getChauffeur();

        // 1. Vérification anti double-acceptation
        List<Matching> allMatchings = matchingRepository.findAllByDemande(demande);
        boolean alreadyAccepted = allMatchings.stream()
                .anyMatch(m -> m.getStatut() == MatchingStatut.ACCEPTED);

        if (alreadyAccepted) {
            throw new RuntimeException("Cette demande a déjà été acceptée par un autre chauffeur");
        }

        // 2. Récupération d'un véhicule actif du chauffeur
        List<Vehicule> vehiculesActifs = vehiculeRepository.findByChauffeur(chauffeur)
                .stream()
                .filter(v -> v.getStatut() == VehiculeStatut.ACTIVE)
                .toList();

        if (vehiculesActifs.isEmpty()) {
            throw new RuntimeException("Le chauffeur n'a aucun véhicule actif");
        }
        Vehicule vehiculeChoisi = vehiculesActifs.get(0);

        // 3. Accepter ce matching
        matching.setStatut(MatchingStatut.ACCEPTED);
        matching.setDateModification(LocalDateTime.now());

        // 4. Expirer TOUS les autres matchings (atomique)
        for (Matching m : allMatchings) {
            if (!m.getIdMatching().equals(matchingId)) {
                m.setStatut(MatchingStatut.EXPIRED);
                m.setDateModification(LocalDateTime.now());
                matchingRepository.save(m);
            }
        }

        // 5. Mettre à jour la DemandeCourse
        demande.setStatut(DemandeStatus.ACCEPTED);
        demandeCoursRepository.save(demande);

        // 6. Création automatique de la Course
        Course course = Course.builder()
                .demande(demande)
                .matching(matching)
                .chauffeur(chauffeur)
                .vehicule(vehiculeChoisi)
                .localisationDepart(demande.getLocalisationDepart())
                .localisationArrivee(demande.getLocalisationArrivee())
                .statut(CourseStatus.ACCEPTED)
                .prixFinal(null)               // calculé plus tard
                .build();

        course = courseRepository.save(course);
        demande.setCourse(course);
        // 7. Lien bidirectionnel
        matching.setCourse(course);
        matchingRepository.save(matching);
// === NOTIFICATION TEMPS RÉEL AU CHAUFFEUR ===
        DriverNotificationDTO notif = DriverNotificationDTO.builder()
                .type("COURSE_ACCEPTED")
                .titre("✅ Course Acceptée !")
                .message("Vous avez une nouvelle course immédiate")
                .courseId(course.getIdCourse())
                .data(course.getPrixFinal())           // tu peux mettre plus d'infos
                .build();

        realTimeController.sendNotificationToDriver(
                course.getChauffeur().getIdChauffeur(),
                notif
        );
        return matching;
    }

    @Override
    @Transactional
    public Matching rejectMatching(Long id) {
        Matching matching = getMatchingById(id);
        if (matching == null) return null;

        matching.setStatut(MatchingStatut.REJECTED);
        return matchingRepository.save(matching);
    }

    /**
     * Méthode qui sera appelée depuis DemandeCoursService
     * quand une demande passe en MATCHING → broadcast aux drivers
     */
    /**
     * Broadcast uniquement aux chauffeurs qui ont AU MOINS un véhicule actif
     */
    /*
    @Override
    @Transactional
    public void proposeMatchingsToAvailableDrivers(DemandeCourse demande) {
        if (demande.getStatut() != DemandeStatus.MATCHING) {
            throw new IllegalStateException("La demande doit être en statut MATCHING");
        }

        // On délègue au CourseService qui contient déjà la logique de proximité
        courseService.createProximityMatchings(demande, 10.0);   // max 10 km

        System.out.println("✅ Matching par proximité terminé pour la demande " + demande.getIdDemande());
    } */
    @Override
    @Transactional
    public List<Matching> proposeMatchingsToAvailableDrivers(DemandeCourse demande) {
        if (demande.getStatut() != DemandeStatus.MATCHING) {
            throw new IllegalStateException("La demande doit être en statut MATCHING");
        }

        // 1) Ta logique actuelle de création des matchings
        courseService.createProximityMatchings(demande, 10.0);

        // 2) Récupérer les matchings PROPOSED de cette demande
        List<Matching> proposed = matchingRepository.findAllByDemande(demande).stream()
                .filter(m -> m.getStatut() == MatchingStatut.PROPOSED)
                .toList();

        // 3) Envoyer une notification WS à chaque chauffeur concerné
        for (Matching m : proposed) {
            Chauffeur ch = m.getChauffeur();
            if (ch == null || ch.getIdChauffeur() == null) continue;

            DriverNotificationDTO notif = DriverNotificationDTO.builder()
                    .type("NEW_COURSE")
                    .titre("Nouvelle demande")
                    .message("Une course vous est proposée")
                    .data(java.util.Map.of(
                            "idMatching", m.getIdMatching(),
                            "idDemande", demande.getIdDemande(),
                            "chauffeurId", ch.getIdChauffeur(),
                            "prixEstime", demande.getPrixEstime() != null ? demande.getPrixEstime() : java.math.BigDecimal.ZERO,
                            "typeVehicule", demande.getTypeVehiculeDemande() != null ? demande.getTypeVehiculeDemande().name() : "ECONOMY",
                            "adresseDepart", demande.getLocalisationDepart() != null && demande.getLocalisationDepart().getAdresse() != null ? demande.getLocalisationDepart().getAdresse() : "Adresse inconnue",
                            "adresseArrivee", demande.getLocalisationArrivee() != null && demande.getLocalisationArrivee().getAdresse() != null ? demande.getLocalisationArrivee().getAdresse() : "Adresse inconnue",
                            "clientNom", demande.getClient() != null && demande.getClient().getUsername() != null ? demande.getClient().getUsername() : "Client"
                    ))
                    .build();

            realTimeController.sendNotificationToDriver(ch.getIdChauffeur(), notif);
        }

        System.out.println("✅ Matchings PROPOSED notifiés: " + proposed.size() + " pour demande " + demande.getIdDemande());
        return proposed;
    }
    @Override
    public List<Matching> getMatchingsByChauffeurId(Long chauffeurId) {
        return matchingRepository.findByChauffeur_IdChauffeur(chauffeurId);
    }
    // MatchingServiceImpl
    @Override
    @Transactional(readOnly = true)
    public List<MatchingDriverCardDTO> getMatchingCardsByChauffeurId(Long chauffeurId) {
        return matchingRepository
                .findDetailedByChauffeurAndStatut(chauffeurId, MatchingStatut.PROPOSED)
                .stream()
                .map(this::toCardDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public MatchingDriverCardDTO getMatchingCardById(Long matchingId) {
        Matching m = matchingRepository.findDetailedById(matchingId).orElse(null);
        return m == null ? null : toCardDto(m);
    }

    private MatchingDriverCardDTO toCardDto(Matching m) {
        DemandeCourse d = m.getDemande();
        return MatchingDriverCardDTO.builder()
                .idMatching(m.getIdMatching())
                .idDemande(d != null ? d.getIdDemande() : null)
                .statut(m.getStatut() != null ? m.getStatut().name() : null)
                .chauffeurId(m.getChauffeur() != null ? m.getChauffeur().getIdChauffeur() : null)
                .prixEstime(d != null ? d.getPrixEstime() : null)
                .typeVehicule(d != null && d.getTypeVehiculeDemande() != null ? d.getTypeVehiculeDemande().name() : null)
                .adresseDepart(d != null && d.getLocalisationDepart() != null ? d.getLocalisationDepart().getAdresse() : null)
                .adresseArrivee(d != null && d.getLocalisationArrivee() != null ? d.getLocalisationArrivee().getAdresse() : null)
                .clientNom(d != null && d.getClient() != null ? d.getClient().getUsername() : null)
                .build();
    }
}


