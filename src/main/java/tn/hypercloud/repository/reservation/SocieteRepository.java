package tn.hypercloud.repository.reservation;

import org.springframework.data.jpa.repository.JpaRepository;
import tn.hypercloud.entity.reservation.Societe;

public interface SocieteRepository extends JpaRepository<Societe, Integer> {
}