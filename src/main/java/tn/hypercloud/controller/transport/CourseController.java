package tn.hypercloud.controller.transport;

import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.*;
import tn.hypercloud.dto.transport.ClientPaymentConfirmRequestDto;
import tn.hypercloud.dto.transport.DriverPaymentVerifyRequestDto;
import tn.hypercloud.dto.transport.PaymentVerificationStatusDto;
import tn.hypercloud.entity.transport.Course;
import tn.hypercloud.entity.transport.enums.CourseStatus;
import tn.hypercloud.service.transport.ICourseService;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/hypercloud/courses")
@AllArgsConstructor
public class CourseController {

    private final ICourseService courseService;


    @PostMapping
    public Course addCourse(@RequestBody Course course) {
        return courseService.addCourse(course);
    }

    @GetMapping("/{id}")
    public Course getCourseById(@PathVariable Long id) {
        return courseService.getCourseById(id);
    }

    @GetMapping
    public List<Course> getAllCourses() {
        return courseService.getAllCourses();
    }


    @PutMapping("/{id}/demarrer")
    public Course startCourse(@PathVariable Long id) {
        return courseService.startCourse(id);
    }

    @PutMapping("/{id}/statut/IN_PROGRESS")
    public Course setInProgress(@PathVariable Long id) {
        return courseService.updateStatut(id, CourseStatus.IN_PROGRESS);
    }

    @PutMapping("/{id}/terminer")
    public Course completeCourse(@PathVariable Long id) {
        return courseService.completeCourse(id);        // → COMPLETED (paiement sécurisé séparé)
    }

    @PostMapping("/{id}/paiement/client-confirmer")
    public PaymentVerificationStatusDto confirmClientPayment(
            @PathVariable Long id,
            @RequestBody(required = false) ClientPaymentConfirmRequestDto request
    ) {
        String paymentIntentId = request != null ? request.getPaymentIntentId() : null;
        return courseService.confirmClientPayment(id, paymentIntentId);
    }

    @PostMapping("/{id}/paiement/valider-chauffeur")
    public PaymentVerificationStatusDto verifyPaymentByDriver(
            @PathVariable Long id,
            @RequestBody DriverPaymentVerifyRequestDto request
    ) {
        return courseService.verifyPaymentByDriver(id, request.getVerificationCode());
    }

    @GetMapping("/{id}/paiement/statut")
    public PaymentVerificationStatusDto getPaymentStatus(@PathVariable Long id) {
        return courseService.getPaymentVerificationStatus(id);
    }

    @GetMapping("/{id}/confirmation-client")
    public Map<String, Object> getClientConfirmationStatus(@PathVariable Long id) {
        return Map.of(
                "courseId", id,
                "clientConfirmed", courseService.isClientConfirmationReceived(id)
        );
    }

    @PutMapping("/{id}/annuler")
    public Course cancelCourse(@PathVariable Long id) {
        return courseService.updateStatut(id, CourseStatus.CANCELLED);
    }

    // =============================================
    // Méthodes supplémentaires utiles pour le frontend
    // =============================================
   /* @GetMapping("/chauffeur/{chauffeurId}")
    public List<Course> getCoursesByChauffeur(@PathVariable Long chauffeurId) {
        // On peut charger le chauffeur si besoin, mais pour l'instant on laisse le service gérer
        return courseService.getCoursesByChauffeur(null); // à adapter si tu veux filtrer par ID
    }
*/
    @GetMapping("/chauffeur/{chauffeurId}")
    public List<Map<String, Object>> getCoursesByChauffeur(@PathVariable Long chauffeurId) {
        return courseService.getCoursesByChauffeur(chauffeurId)
                .stream()
                .map(this::toHistoryPayload)
            .collect(Collectors.toList());
    }

    private Map<String, Object> toHistoryPayload(Course course) {
        Map<String, Object> payload = new LinkedHashMap<>();

        payload.put("idCourse", course.getIdCourse());
        payload.put("statut", course.getStatut() != null ? course.getStatut().name() : null);
        payload.put("prixFinal", course.getPrixFinal());
        payload.put("montantCommission", course.getMontantCommission());
        payload.put("dateCreation", course.getDateCreation());
        payload.put("dateModification", course.getDateModification());

        if (course.getLocalisationDepart() != null) {
            Map<String, Object> departPayload = new LinkedHashMap<>();
            departPayload.put("idLocalisation", course.getLocalisationDepart().getIdLocalisation());
            departPayload.put("latitude", course.getLocalisationDepart().getLatitude());
            departPayload.put("longitude", course.getLocalisationDepart().getLongitude());
            departPayload.put("adresse", course.getLocalisationDepart().getAdresse());
            payload.put("localisationDepart", departPayload);
        }

        if (course.getLocalisationArrivee() != null) {
            Map<String, Object> arriveePayload = new LinkedHashMap<>();
            arriveePayload.put("idLocalisation", course.getLocalisationArrivee().getIdLocalisation());
            arriveePayload.put("latitude", course.getLocalisationArrivee().getLatitude());
            arriveePayload.put("longitude", course.getLocalisationArrivee().getLongitude());
            arriveePayload.put("adresse", course.getLocalisationArrivee().getAdresse());
            payload.put("localisationArrivee", arriveePayload);
        }

        if (course.getDemande() != null) {
            Map<String, Object> demandePayload = new LinkedHashMap<>();
            demandePayload.put("idDemande", course.getDemande().getIdDemande());

            if (course.getDemande().getClient() != null) {
                Map<String, Object> clientPayload = new LinkedHashMap<>();
                clientPayload.put("id", course.getDemande().getClient().getId());
                clientPayload.put("username", course.getDemande().getClient().getUsername());
                clientPayload.put("email", course.getDemande().getClient().getEmail());
                demandePayload.put("client", clientPayload);
            }

            payload.put("demande", demandePayload);
        }

        if (course.getPaiementTransport() != null) {
            Map<String, Object> paiementPayload = new LinkedHashMap<>();
            paiementPayload.put("idPaiement", course.getPaiementTransport().getIdPaiement());
            paiementPayload.put("montantTotal", course.getPaiementTransport().getMontantTotal());
            paiementPayload.put("montantCommission", course.getPaiementTransport().getMontantCommission());
            paiementPayload.put("montantNet", course.getPaiementTransport().getMontantNet());
            paiementPayload.put("statut", course.getPaiementTransport().getStatut());
            paiementPayload.put("datePaiement", course.getPaiementTransport().getDatePaiement());
            paiementPayload.put("dateCreation", course.getPaiementTransport().getDateCreation());
            payload.put("paiementTransport", paiementPayload);
        }

        return payload;
    }
    @GetMapping("/statut/{statut}")
    public List<Course> getCoursesByStatut(@PathVariable String statut) {
        CourseStatus status = CourseStatus.valueOf(statut.toUpperCase());
        return courseService.getCoursesByStatut(status);
    }

    @PutMapping("/{id}/statut/{statut}")
    public Course updateStatut(@PathVariable Long id, @PathVariable String statut) {
        CourseStatus status = CourseStatus.valueOf(statut.toUpperCase());
        return courseService.updateStatut(id, status);
    }

    // Suppression (à utiliser avec prudence)
    @DeleteMapping("/{id}")
    public void deleteCourse(@PathVariable Long id) {
        courseService.deleteCourse(id);
    }
    @GetMapping("/client/{clientId}")
    public List<Course> getCoursesByClient(@PathVariable Long clientId) {
        return courseService.getCoursesByClient(clientId);
    }
}
