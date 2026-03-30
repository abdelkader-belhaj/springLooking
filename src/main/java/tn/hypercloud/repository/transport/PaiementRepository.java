package tn.hypercloud.repository.transport;

import org.springframework.data.jpa.repository.JpaRepository;
import tn.hypercloud.entity.transport.Course;
import tn.hypercloud.entity.transport.Paiement;
import tn.hypercloud.entity.transport.enums.PaiementMethode;
import tn.hypercloud.entity.transport.enums.PaiementStatut;

import java.util.List;
import java.util.Optional;
public interface PaiementRepository extends JpaRepository<Paiement, Long> {
    Optional<Paiement> findByCourse(Course course);
    List<Paiement> findByStatut(PaiementStatut statut);
    List<Paiement> findByMethode(PaiementMethode methode);
}