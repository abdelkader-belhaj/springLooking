package tn.hypercloud.repository.reservation;

import org.springframework.data.jpa.repository.JpaRepository;
import tn.hypercloud.entity.reservation.Vol;
import java.time.LocalDate;
import java.util.List;

public interface VolRepository extends JpaRepository<Vol, Integer> {
    List<Vol> findByDepartAndArriveeAndDateDepart(String depart, String arrivee, LocalDate date);
    List<Vol> findBySocieteId(Integer societeId);
}