package tn.hypercloud.service.transport;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.hypercloud.entity.transport.Chauffeur;
import tn.hypercloud.entity.transport.Course;
import tn.hypercloud.entity.transport.Paiement;
import tn.hypercloud.entity.transport.Vehicule;
import tn.hypercloud.entity.transport.enums.CourseStatus;
import tn.hypercloud.entity.transport.enums.PaiementMethode;
import tn.hypercloud.entity.transport.enums.PaiementStatut;
import tn.hypercloud.repository.transport.CourseRepository;
import tn.hypercloud.repository.transport.PaiementRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

@Service
@AllArgsConstructor
public class CourseServiceImpl implements ICourseService {

    private final CourseRepository courseRepository;
    private final PaiementRepository paiementRepository;
    private final IDistanceService distanceService;   // ton service OSRM

    @Override
    public Course addCourse(Course course) {
        return courseRepository.save(course);
    }

    @Override
    public Course updateCourse(Course course) {
        return courseRepository.save(course);
    }

    @Override
    public void deleteCourse(Long id) {
        courseRepository.deleteById(id);
    }

    @Override
    public Course getCourseById(Long id) {
        return courseRepository.findById(id).orElse(null);
    }

    @Override
    public List<Course> getAllCourses() {
        return courseRepository.findAll();
    }

    @Override
    public List<Course> getCoursesByStatut(CourseStatus statut) {
        return courseRepository.findByStatut(statut);
    }

    @Override
    public List<Course> getCoursesByChauffeur(Chauffeur chauffeur) {
        return courseRepository.findByChauffeur(chauffeur);
    }

    @Override
    @Transactional
    public Course updateStatut(Long id, CourseStatus statut) {
        Course course = getCourseById(id);
        if (course == null) return null;
        course.setStatut(statut);
        return courseRepository.save(course);
    }

    @Override
    public Course startCourse(Long id) {
        return updateStatut(id, CourseStatus.STARTED);
    }

    @Override
    @Transactional
    public Course completeCourse(Long id) {
        Course course = getCourseById(id);
        if (course == null) return null;

        // === CALCUL DU PRIX FINAL ===
        IDistanceService.RouteInfo route = distanceService.calculateRoute(
                course.getLocalisationDepart(),
                course.getLocalisationArrivee()
        );

        BigDecimal prixFinal = calculateFinalPrice(route, course.getVehicule());
        course.setPrixFinal(prixFinal);

        // === CRÉATION AUTOMATIQUE DU PAIEMENT ===
        Paiement paiement = Paiement.builder()
                .course(course)
                .montant(prixFinal)
                .methode(PaiementMethode.CARD)           // tu peux le rendre dynamique plus tard
                .statut(PaiementStatut.COMPLETED)
                .datePaiement(LocalDateTime.now())
                .build();

        paiement = paiementRepository.save(paiement);
        course.setPaiement(paiement);

        course.setStatut(CourseStatus.COMPLETED);
        return courseRepository.save(course);
    }

    private BigDecimal calculateFinalPrice(IDistanceService.RouteInfo route, Vehicule vehicule) {
        BigDecimal baseFee = new BigDecimal("5.00");

        BigDecimal distanceCost = vehicule.getPrixKm()
                .multiply(BigDecimal.valueOf(route.distanceKm()));

        BigDecimal timeCost = vehicule.getPrixMinute()
                .multiply(BigDecimal.valueOf(route.durationMinutes()));

        return baseFee.add(distanceCost).add(timeCost)
                .setScale(2, RoundingMode.HALF_UP);
    }
}
