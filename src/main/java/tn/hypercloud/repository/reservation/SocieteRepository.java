package tn.hypercloud.repository.reservation;

import org.springframework.data.jpa.repository.JpaRepository;
import tn.hypercloud.entity.reservation.Societe;

import java.util.Optional;

public interface SocieteRepository extends JpaRepository<Societe, Integer> {
    Optional<Societe> findByUserId(Long userId);
}