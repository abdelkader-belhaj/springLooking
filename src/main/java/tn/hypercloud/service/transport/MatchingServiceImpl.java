package tn.hypercloud.service.transport;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.hypercloud.controller.transport.RealTimeTransportController;
import tn.hypercloud.dto.transport.DriverNotificationDTO;
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
    @Override
    @Transactional
    public void proposeMatchingsToAvailableDrivers(DemandeCourse demande) {
        if (demande.getStatut() != DemandeStatus.MATCHING) {
            throw new IllegalStateException("La demande doit être en statut MATCHING");
        }

        // On délègue au CourseService qui contient déjà la logique de proximité
        courseService.createProximityMatchings(demande, 10.0);   // max 10 km

        System.out.println("✅ Matching par proximité terminé pour la demande " + demande.getIdDemande());
    }
}


