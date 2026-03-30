package tn.hypercloud.service.transport;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.hypercloud.entity.transport.DemandeCourse;
import tn.hypercloud.entity.transport.Localisation;
import tn.hypercloud.entity.transport.enums.DemandeStatus;
import tn.hypercloud.entity.user.User;
import tn.hypercloud.repository.transport.DemandeCoursRepository;
import tn.hypercloud.repository.transport.LocalisationRepository;
import tn.hypercloud.repository.user.UserRepository;

import java.time.LocalDateTime;
import java.util.List;

@Service
@AllArgsConstructor
public class DemandeCoursServiceImpl implements IDemandeCoursService {

    private final DemandeCoursRepository demandeCoursRepository;
    private final UserRepository userRepository;
    private final LocalisationRepository localisationRepository;
    private final IMatchingService matchingService;        // ← INJECTION

    // ====================== ADD (création par client) ======================
    @Override
    @Transactional
    public DemandeCourse addDemandeCourse(DemandeCourse demandeCourse) {
        // 1. Récupérer le client complet
        if (demandeCourse.getClient() != null && demandeCourse.getClient().getId() != null) {
            User client = userRepository.findById(demandeCourse.getClient().getId())
                    .orElseThrow(() -> new RuntimeException("Client non trouvé avec id: " + demandeCourse.getClient().getId()));
            demandeCourse.setClient(client);
        } else {
            throw new IllegalArgumentException("Client ID est obligatoire");
        }

        // 2. Récupérer localisation départ
        if (demandeCourse.getLocalisationDepart() != null && demandeCourse.getLocalisationDepart().getIdLocalisation() != null) {
            Localisation depart = localisationRepository.findById(demandeCourse.getLocalisationDepart().getIdLocalisation())
                    .orElseThrow(() -> new RuntimeException("Localisation départ non trouvée"));
            demandeCourse.setLocalisationDepart(depart);
        } else {
            throw new IllegalArgumentException("Localisation départ ID obligatoire");
        }

        // 3. Récupérer localisation arrivée
        if (demandeCourse.getLocalisationArrivee() != null && demandeCourse.getLocalisationArrivee().getIdLocalisation() != null) {
            Localisation arrivee = localisationRepository.findById(demandeCourse.getLocalisationArrivee().getIdLocalisation())
                    .orElseThrow(() -> new RuntimeException("Localisation arrivée non trouvée"));
            demandeCourse.setLocalisationArrivee(arrivee);
        } else {
            throw new IllegalArgumentException("Localisation arrivée ID obligatoire");
        }

        // Statut initial = PENDING (comme dans le PDF)
        demandeCourse.setStatut(DemandeStatus.PENDING);

        // Calcul du prix estimé (version simple pour l'instant)
        // TODO : à améliorer avec vraie distance + tarif véhicule
        if (demandeCourse.getPrixEstime() == null) {
            demandeCourse.setPrixEstime(new java.math.BigDecimal("25.00")); // placeholder
        }

        DemandeCourse saved = demandeCoursRepository.save(demandeCourse);
        return saved;
    }

    // ====================== CRUD ======================
    @Override
    public DemandeCourse updateDemandeCourse(DemandeCourse demandeCourse) {
        return demandeCoursRepository.save(demandeCourse);
    }

    @Override
    public void deleteDemandeCourse(Long id) {
        demandeCoursRepository.deleteById(id);
    }

    @Override
    public DemandeCourse getDemandeCoursById(Long id) {
        return demandeCoursRepository.findById(id).orElse(null);
    }

    @Override
    public List<DemandeCourse> getAllDemandeCourses() {
        return demandeCoursRepository.findAll();
    }

    @Override
    public List<DemandeCourse> getDemandesByStatut(DemandeStatus statut) {
        return demandeCoursRepository.findByStatut(statut);
    }

    @Override
    public DemandeCourse updateStatut(Long id, DemandeStatus statut) {
        DemandeCourse demande = getDemandeCoursById(id);
        if (demande == null) return null;
        demande.setStatut(statut);
        return demandeCoursRepository.save(demande);
    }

    // ====================== WORKFLOW MATCHING ======================
    @Override
    @Transactional
    public DemandeCourse startMatching(Long id) {
        DemandeCourse demande = getDemandeCoursById(id);
        if (demande == null) {
            throw new RuntimeException("Demande de course non trouvée");
        }

        if (demande.getStatut() != DemandeStatus.PENDING) {
            throw new IllegalStateException("Seules les demandes en statut PENDING peuvent passer en MATCHING");
        }

        // 1. Passage en MATCHING
        demande.setStatut(DemandeStatus.MATCHING);
        demande.setDateModification(LocalDateTime.now());
        DemandeCourse saved = demandeCoursRepository.save(demande);

        // 2. Déclenchement du broadcast (création des N Matching PROPOSED)
        matchingService.proposeMatchingsToAvailableDrivers(saved);

        return saved;
    }
}
