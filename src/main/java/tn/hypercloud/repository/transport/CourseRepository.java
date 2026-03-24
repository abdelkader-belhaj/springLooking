package tn.hypercloud.repository.transport;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tn.hypercloud.entity.transport.Course;

@Repository
public interface CourseRepository extends JpaRepository<Course, Long> {
}
