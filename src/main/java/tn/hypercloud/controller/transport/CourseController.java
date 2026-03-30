package tn.hypercloud.controller.transport;

import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.*;
import tn.hypercloud.entity.transport.Course;
import tn.hypercloud.entity.transport.enums.CourseStatus;
import tn.hypercloud.service.transport.ICourseService;

import java.util.List;

@RestController
@RequestMapping("/hypercloud/courses")
@AllArgsConstructor
public class CourseController {

    private final ICourseService courseService;

    // =============================================
    // CRUD de base
    // =============================================
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

    // =============================================
    // POINT 5 DU PDF : Déroulement de la course
    // =============================================
    @PutMapping("/{id}/demarrer")
    public Course startCourse(@PathVariable Long id) {
        return courseService.startCourse(id);           // → ACCEPTED → STARTED
    }

    @PutMapping("/{id}/statut/IN_PROGRESS")
    public Course setInProgress(@PathVariable Long id) {
        return courseService.updateStatut(id, CourseStatus.IN_PROGRESS);
    }

    @PutMapping("/{id}/terminer")
    public Course completeCourse(@PathVariable Long id) {
        return courseService.completeCourse(id);        // → COMPLETED + déclenche paiement
    }

    @PutMapping("/{id}/annuler")
    public Course cancelCourse(@PathVariable Long id) {
        return courseService.updateStatut(id, CourseStatus.CANCELLED);
    }

    // =============================================
    // Méthodes supplémentaires utiles pour le frontend
    // =============================================
    @GetMapping("/chauffeur/{chauffeurId}")
    public List<Course> getCoursesByChauffeur(@PathVariable Long chauffeurId) {
        // On peut charger le chauffeur si besoin, mais pour l'instant on laisse le service gérer
        return courseService.getCoursesByChauffeur(null); // à adapter si tu veux filtrer par ID
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
}
