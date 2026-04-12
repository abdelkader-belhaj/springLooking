package tn.hypercloud.controller.transport;

import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.*;
import tn.hypercloud.dto.transport.ClientPaymentConfirmRequestDto;
import tn.hypercloud.dto.transport.DriverPaymentVerifyRequestDto;
import tn.hypercloud.dto.transport.PaymentVerificationStatusDto;
import tn.hypercloud.entity.transport.Course;
import tn.hypercloud.entity.transport.enums.CourseStatus;
import tn.hypercloud.service.transport.ICourseService;

import java.util.List;

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
    public List<Course> getCoursesByChauffeur(@PathVariable Long chauffeurId) {
        return courseService.getCoursesByChauffeur(chauffeurId);
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
