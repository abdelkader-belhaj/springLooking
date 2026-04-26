package tn.hypercloud.repository.accommodation;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tn.hypercloud.entity.accommodation.Categorie;
import java.util.List;

@Repository
public interface CategorieRepository
        extends JpaRepository<Categorie, Integer> {

    boolean existsByNomCategorie(String nomCategorie);
    boolean existsByNomCategorieAndOwnerId(String nomCategorie, Long ownerId);
    boolean existsByNomCategorieAndOwnerIsNull(String nomCategorie);
    List<Categorie> findByStatutTrue();
    List<Categorie> findByOwnerId(Long ownerId);
    List<Categorie> findByOwnerIdAndStatutTrue(Long ownerId);
}