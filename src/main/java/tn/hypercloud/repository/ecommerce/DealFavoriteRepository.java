package tn.hypercloud.repository.ecommerce;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tn.hypercloud.entity.ecommerce.DealFavorite;

@Repository
public interface DealFavoriteRepository extends JpaRepository<DealFavorite, Long> {
}
