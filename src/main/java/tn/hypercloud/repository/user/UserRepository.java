package tn.hypercloud.repository.user;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tn.hypercloud.entity.user.Role;
import tn.hypercloud.entity.user.User;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA genere automatiquement le SQL
 * Pas besoin d'ecrire les requetes — le nom de la methode suffit
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    // SELECT * FROM users WHERE email = ?
    Optional<User> findByEmail(String email);

    // SELECT * FROM users WHERE lower(email) = lower(?)
    Optional<User> findByEmailIgnoreCase(String email);

    // SELECT * FROM users WHERE google_sub = ?
    Optional<User> findByGoogleSub(String googleSub);

    // SELECT * FROM users WHERE username = ?
    Optional<User> findByUsername(String username);

    // SELECT COUNT(*) FROM users WHERE email = ? > 0
    boolean existsByEmail(String email);

    // SELECT COUNT(*) FROM users WHERE username = ? > 0
    boolean existsByUsername(String username);

    // SELECT * FROM users WHERE role = ?
    List<User> findByRole(Role role);

    // SELECT * FROM users WHERE enabled = ?
    List<User> findByEnabled(boolean enabled);
}