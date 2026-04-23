package tn.hypercloud.repository.forum;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import tn.hypercloud.entity.forum.Community;

import java.util.List;

@Repository
public interface CommunityRepository extends JpaRepository<Community, Long> {

    @Query("SELECT c FROM Community c LEFT JOIN FETCH c.forums")
    List<Community> findAllWithForums();
}