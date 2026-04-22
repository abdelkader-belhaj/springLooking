package tn.hypercloud.repository.event;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tn.hypercloud.entity.event.EventCategory;
import java.util.List;

@Repository
public interface EventCategoryRepository
        extends JpaRepository<EventCategory, Integer> {

    // Trouver par type
    List<EventCategory> findByType(
            EventCategory.CategoryType type);

    // Vérifier si nom existe déjà
    boolean existsByName(String name);
}