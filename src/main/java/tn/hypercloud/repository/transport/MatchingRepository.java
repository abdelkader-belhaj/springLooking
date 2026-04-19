package tn.hypercloud.repository.transport;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tn.hypercloud.entity.transport.Matching;

@Repository
public interface MatchingRepository extends JpaRepository<Matching, Long> {
}
