package tn.hypercloud.repository.forum;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tn.hypercloud.entity.forum.Community;

@Repository
public interface CommunityRepository extends JpaRepository<Community, Long> {
}
