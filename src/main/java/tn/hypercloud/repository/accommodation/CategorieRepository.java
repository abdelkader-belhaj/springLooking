package tn.hypercloud.repository.accommodation;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tn.hypercloud.entity.accommodation.Categorie;

@Repository
public interface CategorieRepository extends JpaRepository<Categorie, Integer> {
}
