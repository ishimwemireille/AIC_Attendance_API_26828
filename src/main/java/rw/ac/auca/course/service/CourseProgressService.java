package rw.ac.auca.course.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rw.ac.auca.attendance.domain.Attendance;
import rw.ac.auca.attendance.domain.AttendanceStatus;
import rw.ac.auca.attendance.repository.AttendanceRepository;
import rw.ac.auca.course.domain.Course;
import rw.ac.auca.course.domain.CourseActivity;
import rw.ac.auca.course.repository.CourseActivityRepository;
import rw.ac.auca.exception.BusinessRuleException;
import rw.ac.auca.session.domain.Session;
import rw.ac.auca.session.repository.SessionRepository;
import rw.ac.auca.user.service.CurrentUserService;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * The Class CourseProgressService. The numbers behind a trainer's dashboard.
 *
 * Everything here is derived from records that already exist, so nothing can
 * drift out of step with the attendance that produced it.
 *
 * @author Ishimwe Mireille
 * @version 1.0
 */
@Service
@Transactional(readOnly = true)
public class CourseProgressService {

    /** Below this, a student is at risk of losing exam eligibility. */
    public static final int THRESHOLD = 75;

    @Autowired
    private CourseService courseService;

    @Autowired
    private SessionRepository sessionRepository;

    @Autowired
    private AttendanceRepository attendanceRepository;

    @Autowired
    private CourseActivityRepository courseActivityRepository;

    @Autowired
    private CurrentUserService currentUserService;

    public CourseProgress progressOf(UUID courseId) {
        // findCourseById already refuses a course a trainer does not lead.
        Course course = courseService.findCourseById(courseId);

        List<Session> sessions = sessionRepository.findByCourseIdOrderBySessionDateAscStartTimeAsc(courseId);
        List<Attendance> records = attendanceRepository.findBySessionCourseId(courseId);

        CourseProgress progress = new CourseProgress();
        progress.setCourse(course);
        progress.setSessionsPlanned(sessions.size());
        progress.setRecordsWritten(records.size());

        long delivered = records.stream()
                .map(record -> record.getSession().getId())
                .distinct()
                .count();
        progress.setSessionsDelivered((int) delivered);
        progress.setAttendanceRate(rate(records));
        progress.setElapsedPercent(elapsed(course));

        Map<UUID, List<Attendance>> byStudent = records.stream()
                .collect(Collectors.groupingBy(record -> record.getStudent().getId()));

        byStudent.forEach((studentId, theirs) -> {
            int studentRate = rate(theirs);
            if (studentRate >= THRESHOLD) {
                return;
            }
            Attendance any = theirs.get(0);
            CourseProgress.StudentStanding standing = new CourseProgress.StudentStanding();
            standing.setStudentId(studentId.toString());
            standing.setRegNumber(any.getStudent().getRegNumber());
            standing.setFullName(any.getStudent().getFullName());
            standing.setTotal(theirs.size());
            standing.setAttended((int) theirs.stream()
                    .filter(record -> record.getStatus() != AttendanceStatus.ABSENT)
                    .count());
            standing.setRate(studentRate);
            progress.getAtRisk().add(standing);
        });

        progress.getAtRisk().sort((a, b) -> Integer.compare(a.getRate(), b.getRate()));
        return progress;
    }

    /** Present, late and excused all count as attended; only ABSENT does not. */
    private int rate(List<Attendance> records) {
        if (records.isEmpty()) {
            return 0;
        }
        long attended = records.stream()
                .filter(record -> record.getStatus() != AttendanceStatus.ABSENT)
                .count();
        return (int) Math.round((attended * 100.0) / records.size());
    }

    private int elapsed(Course course) {
        long total = ChronoUnit.DAYS.between(course.getStartDate(), course.getEndDate());
        if (total <= 0) {
            return 100;
        }
        long done = ChronoUnit.DAYS.between(course.getStartDate(), LocalDate.now());
        return (int) Math.max(0, Math.min(100, Math.round((done * 100.0) / total)));
    }

    public List<CourseActivity> activitiesOf(UUID courseId) {
        courseService.findCourseById(courseId);
        return courseActivityRepository.findByCourseIdOrderByActivityDateDesc(courseId);
    }

    @Transactional
    public CourseActivity logActivity(UUID courseId, CourseActivityRequest request) {
        // findCourseById refuses a course this trainer does not lead.
        Course course = courseService.findCourseById(courseId);

        if (request.getActivityDate().isBefore(course.getStartDate())
                || request.getActivityDate().isAfter(course.getEndDate())) {
            throw new BusinessRuleException("The date must fall inside \"" + course.getTitle()
                    + "\", which runs from " + course.getStartDate()
                    + " to " + course.getEndDate() + ".");
        }

        CourseActivity activity = new CourseActivity();
        activity.setCourse(course);
        activity.setActivityDate(request.getActivityDate());
        activity.setTitle(request.getTitle());
        activity.setNotes(request.getNotes());
        activity.setCreatedBy(currentUserService.require());
        return courseActivityRepository.save(activity);
    }
}
