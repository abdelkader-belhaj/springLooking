package tn.hypercloud.repository.reservation;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tn.hypercloud.entity.reservation.Offre;
import java.util.Optional;

@Repository
public interface OffreRepository extends JpaRepository<Offre, Integer> {
    Optional<Offre> findByCodeAndActifTrue(String code);
}
