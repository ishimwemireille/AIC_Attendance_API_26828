package rw.ac.auca.course.service;

import rw.ac.auca.course.domain.Course;

import java.util.List;
import java.util.UUID;

/**
 * The Interface CourseService.
 *
 * @author Ishimwe Mireille
 * @version 1.0
 */
public interface CourseService {

    Course createCourse(CourseRequest request);

    Course updateCourse(UUID id, CourseRequest request);

    void deleteCourse(UUID id);

    Course findCourseById(UUID id);

    List<Course> findAllCourses();

    List<Course> findByTrainer(UUID trainerId);
}
