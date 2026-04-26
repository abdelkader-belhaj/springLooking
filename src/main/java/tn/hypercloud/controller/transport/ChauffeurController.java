package tn.hypercloud.controller.transport;

import lombok.AllArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.hypercloud.dto.transport.LocationUpdateDTO;
import tn.hypercloud.entity.transport.Chauffeur;
import tn.hypercloud.entity.transport.Course;
import tn.hypercloud.entity.transport.Localisation;
import tn.hypercloud.entity.transport.enums.CourseStatus;
import tn.hypercloud.repository.transport.CourseRepository;
import tn.hypercloud.service.transport.IChauffeurService;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/hypercloud/chauffeurs")
@AllArgsConstructor
public class ChauffeurController {

    private final IChauffeurService chauffeurService;
    private final SimpMessagingTemplate messagingTemplate;
    private final CourseRepository courseRepository;

    @PostMapping
    public Chauffeur addChauffeur(@RequestBody Chauffeur chauffeur) {
        return chauffeurService.addChauffeur(chauffeur);
    }

    @PutMapping("/{id}")
    public Chauffeur updateChauffeur(@PathVariable Long id, @RequestBody Chauffeur chauffeur) {
        chauffeur.setIdChauffeur(id);
        return chauffeurService.updateChauffeur(chauffeur);
    }

    @DeleteMapping("/{id}")
    public void deleteChauffeur(@PathVariable Long id) {
        chauffeurService.deleteChauffeur(id);
    }

    @GetMapping("/{id}")
    public Chauffeur getChauffeurById(@PathVariable Long id) {
        return chauffeurService.getChauffeurById(id);
    }

    @GetMapping
    public List<Chauffeur> getAllChauffeurs() {
        return chauffeurService.getAllChauffeurs();
    }

    // ========== RECHERCHES SPÉCIFIQUES ==========
    @GetMapping("/disponibles")
    public List<Chauffeur> getAvailableChauffeurs() {
        return chauffeurService.getAvailableChauffeurs();
    }

    @GetMapping("/actifs")
    public List<Chauffeur> getActiveChauffeurs() {
        return chauffeurService.getActiveChauffeurs();
    }

    // ========== CHANGEMENTS DE STATUT ==========
    @PutMapping("/{id}/approuver")
    public Chauffeur approveChauffeur(@PathVariable Long id) {
        return chauffeurService.approveChauffeur(id);
    }

    @PutMapping("/{id}/suspendre")
    public Chauffeur suspendChauffeur(@PathVariable Long id) {
        return chauffeurService.suspendChauffeur(id);
    }

    // ========== GESTION ONLINE/OFFLINE ==========
    @PutMapping("/{id}/online")
    public Chauffeur goOnline(@PathVariable Long id) {
        return chauffeurService.goOnline(id);
    }

    @PutMapping("/{id}/offline")
    public Chauffeur goOffline(@PathVariable Long id) {
        return chauffeurService.goOffline(id);
    }

    @PutMapping("/{id}/on-ride")
    public Chauffeur setOnRide(@PathVariable Long id) {
        return chauffeurService.setOnRide(id);
    }

    // ========== ASSOCIATION VÉHICULE ==========
    @PutMapping("/{idChauffeur}/affecter-vehicule/{idVehicule}")
    public Chauffeur affecterChauffeurAVehicule(@PathVariable Long idChauffeur, @PathVariable Long idVehicule) {
        return chauffeurService.affecterChauffeurAVehicule(idChauffeur, idVehicule);
    }
    @PutMapping("/{id}/position")
    public ResponseEntity<Chauffeur> updateDriverPosition(
            @PathVariable Long id,
            @RequestBody Localisation position) {

        Chauffeur updated = chauffeurService.updatePosition(id, position);

        // Push temps reel pour dashboard chauffeur et cartes globales.
        if (updated.getPositionActuelle() != null) {
            LocationUpdateDTO payload = LocationUpdateDTO.builder()
                    .chauffeurId(updated.getIdChauffeur())
                    .actorType("CHAUFFEUR")
                    .latitude(updated.getPositionActuelle().getLatitude())
                    .longitude(updated.getPositionActuelle().getLongitude())
                    .build();

            messagingTemplate.convertAndSend("/topic/chauffeur/" + updated.getIdChauffeur() + "/location", payload);
            messagingTemplate.convertAndSend("/topic/chauffeurs/location", payload);

                Optional<Course> activeCourse = courseRepository
                    .findTopByChauffeur_IdChauffeurAndStatutInOrderByDateModificationDesc(
                        updated.getIdChauffeur(),
                        Arrays.asList(CourseStatus.ACCEPTED, CourseStatus.STARTED, CourseStatus.IN_PROGRESS)
                    );
                activeCourse.ifPresent(course -> messagingTemplate.convertAndSend(
                    "/topic/course/" + course.getIdCourse() + "/location",
                    LocationUpdateDTO.builder()
                        .courseId(course.getIdCourse())
                        .chauffeurId(updated.getIdChauffeur())
                        .actorType("CHAUFFEUR")
                        .latitude(updated.getPositionActuelle().getLatitude())
                        .longitude(updated.getPositionActuelle().getLongitude())
                        .build()
                ));
        }

        return ResponseEntity.ok(updated);
    }
    @GetMapping("/utilisateur/{userId}")
    public Chauffeur getByUtilisateurId(@PathVariable Long userId) {
        return chauffeurService.getChauffeurByUtilisateurId(userId);
    }
}