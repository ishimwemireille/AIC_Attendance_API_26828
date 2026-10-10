package rw.ac.auca.course.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import rw.ac.auca.course.domain.CourseActivity;

import java.util.List;
import java.util.UUID;

/**
 * The Interface CourseActivityRepository.
 *
 * @author Ishimwe Mireille
 * @version 1.0
 */
@Repository
public interface CourseActivityRepository extends JpaRepository<CourseActivity, UUID> {

    List<CourseActivity> findByCourseIdOrderByActivityDateDesc(UUID courseId);
}
