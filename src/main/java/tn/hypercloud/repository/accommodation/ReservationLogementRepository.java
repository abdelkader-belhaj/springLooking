package tn.hypercloud.repository.accommodation;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tn.hypercloud.entity.accommodation.ReservationLogement;
import java.util.List;

@Repository
public interface ReservationLogementRepository
        extends JpaRepository<ReservationLogement, Integer> {

    List<ReservationLogement> findByClientId(Long idClient);
    List<ReservationLogement> findByLogementIdLogement(Integer idLogement);
    List<ReservationLogement> findByLogementHebergeurId(Long idHebergeur);
    List<ReservationLogement> findByArchivedTrue();
    List<ReservationLogement> findByArchivedFalse();
}