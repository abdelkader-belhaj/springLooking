package tn.hypercloud.repository.reservation;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tn.hypercloud.entity.reservation.Societe;

@Repository
public interface SocieteRepository extends JpaRepository<Societe, Integer> {
}
