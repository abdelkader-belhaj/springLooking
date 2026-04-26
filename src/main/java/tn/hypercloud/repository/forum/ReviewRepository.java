package tn.hypercloud.repository.forum;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tn.hypercloud.entity.forum.Review;




@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {
    List<Review> findByForumId(Long forumId);

}
