package tn.hypercloud.repository.transport;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tn.hypercloud.entity.transport.DemandeCourse;

@Repository
public interface DemandeCourseRepository extends JpaRepository<DemandeCourse, Long> {
}
