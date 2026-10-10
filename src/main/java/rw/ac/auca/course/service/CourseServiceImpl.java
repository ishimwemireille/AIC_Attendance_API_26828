package rw.ac.auca.course.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rw.ac.auca.audit.service.AuditService;
import rw.ac.auca.course.domain.Course;
import rw.ac.auca.course.repository.CourseRepository;
import rw.ac.auca.exception.BusinessRuleException;
import rw.ac.auca.exception.ResourceNotFoundException;
import rw.ac.auca.session.repository.SessionRepository;
import rw.ac.auca.user.domain.RoleName;
import rw.ac.auca.user.domain.User;
import rw.ac.auca.user.service.CurrentUserService;
import rw.ac.auca.user.service.UserService;

import java.util.List;
import java.util.UUID;

/**
 * The Class CourseServiceImpl.
 *
 * The cohort takes one course at a time, so the rule that matters here is that
 * two courses may not run on overlapping dates. That spans rows, so no column
 * constraint can express it and it is enforced in this layer.
 *
 * @author Ishimwe Mireille
 * @version 1.0
 */
@Service
@Transactional
public class CourseServiceImpl implements CourseService {

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private SessionRepository sessionRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private CurrentUserService currentUserService;

    @Autowired
    private AuditService auditService;

    @Override
    public Course createCourse(CourseRequest request) {
        Course course = new Course();
        apply(course, request, null);
        Course saved = courseRepository.save(course);
        auditService.record("Course", saved.getId(), AuditService.CREATE, null, saved);
        return saved;
    }

    @Override
    public Course updateCourse(UUID id, CourseRequest request) {
        Course found = findCourseById(id);
        apply(found, request, id);
        Course saved = courseRepository.save(found);
        auditService.record("Course", saved.getId(), AuditService.UPDATE, null, saved);
        return saved;
    }

    private void apply(Course course, CourseRequest request, UUID currentId) {
        if (!request.getEndDate().isAfter(request.getStartDate())) {
            throw new BusinessRuleException("The end date must be after the start date.");
        }

        User trainer = userService.findUserById(request.getTrainerId());
        if (!trainer.hasRole(RoleName.TRAINER)) {
            throw new BusinessRuleException(trainer.getFullName()
                    + " cannot lead a course because they do not hold the TRAINER role.");
        }
        if (!trainer.isActive()) {
            throw new BusinessRuleException(trainer.getFullName()
                    + " is not an active user and cannot lead a course.");
        }

        // The cohort takes one course at a time.
        List<Course> clashing = currentId == null
                ? courseRepository.findByStartDateLessThanEqualAndEndDateGreaterThanEqual(
                        request.getEndDate(), request.getStartDate())
                : courseRepository.findByStartDateLessThanEqualAndEndDateGreaterThanEqualAndIdNot(
                        request.getEndDate(), request.getStartDate(), currentId);
        if (!clashing.isEmpty()) {
            Course first = clashing.get(0);
            throw new BusinessRuleException("These dates overlap \"" + first.getTitle()
                    + "\", which runs from " + first.getStartDate() + " to " + first.getEndDate()
                    + ". The cohort takes one course at a time.");
        }

        course.setCode(request.getCode().trim().toUpperCase());
        course.setTitle(request.getTitle());
        course.setDescription(request.getDescription());
        course.setSequence(request.getSequence());
        course.setStartDate(request.getStartDate());
        course.setEndDate(request.getEndDate());
        course.setTrainer(trainer);
        course.setActive(request.isActive());
    }

    @Override
    public void deleteCourse(UUID id) {
        Course found = findCourseById(id);
        long sessions = sessionRepository.countByCourseId(id);
        if (sessions > 0) {
            throw new BusinessRuleException("Cannot delete \"" + found.getTitle()
                    + "\" because " + sessions + " session(s) belong to it.");
        }
        auditService.record("Course", found.getId(), AuditService.DELETE, found, null);
        courseRepository.delete(found);
    }

    @Override
    @Transactional(readOnly = true)
    public Course findCourseById(UUID id) {
        Course found = courseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("No course found with id " + id));
        if (currentUserService.isTrainerOnly()
                && !found.getTrainer().getId().equals(currentUserService.require().getId())) {
            throw new AccessDeniedException("You may only view courses you lead.");
        }
        return found;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Course> findAllCourses() {
        if (currentUserService.isTrainerOnly()) {
            return findByTrainer(currentUserService.require().getId());
        }
        return courseRepository.findAllByOrderBySequenceAsc();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Course> findByTrainer(UUID trainerId) {
        return courseRepository.findByTrainerIdOrderBySequenceAsc(trainerId);
    }
}
