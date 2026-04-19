package tn.hypercloud.repository.accommodation;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tn.hypercloud.entity.accommodation.Logement;
import java.util.List;

@Repository
public interface LogementRepository
        extends JpaRepository<Logement, Integer> {

    List<Logement> findByDisponibleTrue();
    List<Logement> findByCategorieIdCategorie(Integer idCategorie);
    List<Logement> findByHebergeurId(Long idHebergeur);
}