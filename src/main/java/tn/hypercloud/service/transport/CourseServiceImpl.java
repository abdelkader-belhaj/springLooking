package tn.hypercloud.service.transport;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.hypercloud.entity.transport.*;
import tn.hypercloud.entity.transport.enums.CourseStatus;
import tn.hypercloud.entity.transport.enums.PaiementMethode;
import tn.hypercloud.entity.transport.enums.PaiementStatut;
import tn.hypercloud.entity.transport.enums.TransactionType;
import tn.hypercloud.repository.transport.ChauffeurRepository;
import tn.hypercloud.repository.transport.CourseRepository;
import tn.hypercloud.repository.transport.PaiementRepository;
import tn.hypercloud.repository.transport.WalletTransactionRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

@Service
@AllArgsConstructor
public class CourseServiceImpl implements ICourseService {

    private final CourseRepository courseRepository;
    private final PaiementRepository paiementRepository;
    private final IDistanceService distanceService;// ton service OSRM
    private final ChauffeurRepository chauffeurRepository;           // ← NOUVEAU
    private final WalletTransactionRepository walletTransactionRepository; // ← NOUVEAU

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

        // Calcul prix final
        IDistanceService.RouteInfo route = distanceService.calculateRoute(
                course.getLocalisationDepart(), course.getLocalisationArrivee());
        BigDecimal prixFinal = calculateFinalPrice(route, course.getVehicule());
        course.setPrixFinal(prixFinal);

        // Création paiement (commission 20 % automatique via @PrePersist)
        PaiementTransport paiementTransport = PaiementTransport.builder()
                .course(course)
                .montantTotal(prixFinal)
                .methode(PaiementMethode.CARD)
                .statut(PaiementStatut.COMPLETED)
                .datePaiement(LocalDateTime.now())
                .build();

        paiementTransport = paiementRepository.save(paiementTransport);

        // === MISE À JOUR SOLDE CHAUFFEUR + TRANSACTION ===
        BigDecimal montantNet = paiementTransport.getMontantNet();
        Chauffeur chauffeur = course.getChauffeur();

// Protection null-safe + mise à jour du solde
        BigDecimal nouveauSolde = (chauffeur.getSolde() != null ? chauffeur.getSolde() : BigDecimal.ZERO)
                .add(montantNet);
        chauffeur.setSolde(nouveauSolde);
        chauffeurRepository.save(chauffeur);
        walletTransactionRepository.save(WalletTransaction.builder()
                .chauffeur(chauffeur)
                .montant(montantNet)
                .type(TransactionType.CREDIT_COURSE)
                .description("Course #" + course.getIdCourse())
                .paiementTransport(paiementTransport)
                .build());

        // Mise à jour course
        course.setPaiementTransport(paiementTransport);
        course.setMontantCommission(paiementTransport.getMontantCommission());
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
