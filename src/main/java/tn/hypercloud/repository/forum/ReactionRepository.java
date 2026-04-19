package tn.hypercloud.repository.forum;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tn.hypercloud.entity.forum.Reaction;

@Repository
public interface ReactionRepository extends JpaRepository<Reaction, Long> {
}
