package tn.hypercloud.repository.ecommerce;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tn.hypercloud.entity.ecommerce.DealFavorite;
import java.util.List;
import java.util.Optional;
import org.springframework.transaction.annotation.Transactional;


@Repository
public interface DealFavoriteRepository extends JpaRepository<DealFavorite, Long> {
    boolean existsByUser_IdAndDeal_Id(Long userId, Long dealId);
    @Transactional
    void deleteByUser_IdAndDeal_Id(Long userId, Long dealId);
    List<DealFavorite> findByUser_Id(Long userId);
    Optional<DealFavorite> findByUser_IdAndDeal_Id(Long userId, Long dealId);
    long countByDeal_Id(Long dealId);
}
