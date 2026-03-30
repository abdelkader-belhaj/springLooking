package tn.hypercloud.service.transport;

import tn.hypercloud.entity.transport.Chauffeur;
import tn.hypercloud.entity.transport.Course;
import tn.hypercloud.entity.transport.enums.CourseStatus;

import java.util.List;
public interface ICourseService {
    Course addCourse(Course course);
    Course updateCourse(Course course);
    void deleteCourse(Long id);
    Course getCourseById(Long id);
    List<Course> getAllCourses();
    List<Course> getCoursesByStatut(CourseStatus statut);
    List<Course> getCoursesByChauffeur(Chauffeur chauffeur);
    Course updateStatut(Long id, CourseStatus statut);
    Course startCourse(Long id);
    Course completeCourse(Long id);
}