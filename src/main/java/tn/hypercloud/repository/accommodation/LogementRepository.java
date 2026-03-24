package tn.hypercloud.repository.accommodation;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tn.hypercloud.entity.accommodation.Logement;

@Repository
public interface LogementRepository extends JpaRepository<Logement, Integer> {
}
