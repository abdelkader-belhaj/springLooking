package tn.hypercloud.repository.user;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tn.hypercloud.entity.user.User;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
}
