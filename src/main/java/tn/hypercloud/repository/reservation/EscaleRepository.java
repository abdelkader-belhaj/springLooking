package tn.hypercloud.repository.reservation;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tn.hypercloud.entity.reservation.Escale;
import java.util.List;

@Repository
public interface EscaleRepository extends JpaRepository<Escale, Integer> {
    List<Escale> findByVolId(Integer volId);
}
