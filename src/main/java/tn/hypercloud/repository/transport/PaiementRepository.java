package tn.hypercloud.repository.transport;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import tn.hypercloud.entity.transport.Course;
import tn.hypercloud.entity.transport.PaiementTransport;
import tn.hypercloud.entity.transport.enums.PaiementMethode;
import tn.hypercloud.entity.transport.enums.PaiementStatut;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
public interface PaiementRepository extends JpaRepository<PaiementTransport, Long> {
    Optional<PaiementTransport> findByCourse(Course course);
    List<PaiementTransport> findByStatut(PaiementStatut statut);
    List<PaiementTransport> findByMethode(PaiementMethode methode);

    @Query("SELECT COALESCE(SUM(p.montantCommission), 0) FROM PaiementTransport  p WHERE p.statut = :statut")
    BigDecimal sumMontantCommissionByStatut(@Param("statut") PaiementStatut statut);

}