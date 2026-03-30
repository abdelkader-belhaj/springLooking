package tn.hypercloud.controller.transport;

import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.*;
import tn.hypercloud.entity.transport.DemandeCourse;
import tn.hypercloud.entity.transport.enums.DemandeStatus;
import tn.hypercloud.entity.user.User;
import tn.hypercloud.service.transport.IDemandeCoursService;
import tn.hypercloud.service.transport.ITransportationBookingService;

import java.util.List;

@RestController
@RequestMapping("/hypercloud/demandes-courses")
@AllArgsConstructor
public class DemandeCoursController {

    private final ITransportationBookingService transportationBookingService;
    private final IDemandeCoursService demandeCoursService;   // pour les opérations de base

    // =============================================
    // POINT 3 PDF : Création d’une demande par le client
    // =============================================
    @PostMapping
    public DemandeCourse createDemandeCourse(@RequestBody DemandeCourse demandeCourse) {
        return transportationBookingService.createBookingRequest(demandeCourse);
    }

    // =============================================
    // POINT 4 PDF : Lancer le matching (broadcast)
    // =============================================
    @PutMapping("/{id}/matching")
    public DemandeCourse startMatching(@PathVariable Long id) {
        return transportationBookingService.startMatching(id);
    }

    // =============================================
    // Opérations de base (CRUD + recherches)
    // =============================================
    @GetMapping("/{id}")
    public DemandeCourse getDemandeCoursById(@PathVariable Long id) {
        return demandeCoursService.getDemandeCoursById(id);
    }

    @GetMapping
    public List<DemandeCourse> getAllDemandeCourses() {
        return demandeCoursService.getAllDemandeCourses();
    }

    @GetMapping("/client/{clientId}")
    public List<DemandeCourse> getDemandesByClient(@PathVariable Long clientId) {
        User client = new User();           // on charge juste l'ID
        client.setId(clientId);
        return transportationBookingService.getBookingsByClient(client);
    }

    @GetMapping("/statut/{statut}")
    public List<DemandeCourse> getDemandesByStatut(@PathVariable String statut) {
        DemandeStatus status = DemandeStatus.valueOf(statut.toUpperCase());
        return demandeCoursService.getDemandesByStatut(status);
    }

    @PutMapping("/{id}/statut/{statut}")
    public DemandeCourse updateStatut(@PathVariable Long id, @PathVariable String statut) {
        DemandeStatus status = DemandeStatus.valueOf(statut.toUpperCase());
        return demandeCoursService.updateStatut(id, status);
    }

    // (Optionnel) Suppression – à utiliser avec prudence
    @DeleteMapping("/{id}")
    public void deleteDemandeCourse(@PathVariable Long id) {
        demandeCoursService.deleteDemandeCourse(id);
    }
}
