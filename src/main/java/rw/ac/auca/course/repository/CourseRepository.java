package rw.ac.auca.course.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import rw.ac.auca.course.domain.Course;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * The Interface CourseRepository.
 *
 * @author Ishimwe Mireille
 * @version 1.0
 */
@Repository
public interface CourseRepository extends JpaRepository<Course, UUID> {

    List<Course> findAllByOrderBySequenceAsc();

    List<Course> findByTrainerIdOrderBySequenceAsc(UUID trainerId);

    Optional<Course> findByCodeIgnoreCase(String code);

    /** Any course whose dates overlap the range given, ignoring one id on update. */
    List<Course> findByStartDateLessThanEqualAndEndDateGreaterThanEqualAndIdNot(
            LocalDate end, LocalDate start, UUID id);

    List<Course> findByStartDateLessThanEqualAndEndDateGreaterThanEqual(
            LocalDate end, LocalDate start);
}
