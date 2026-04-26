package tn.hypercloud.repository.accommodation;

import org.springframework.data.jpa.repository.JpaRepository;
import tn.hypercloud.entity.accommodation.UserPreferences;

import java.util.Optional;

public interface UserPreferencesRepository extends JpaRepository<UserPreferences, Long> {
    Optional<UserPreferences> findByUserId(Integer userId);
}
