package rw.ac.auca.session.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import rw.ac.auca.audit.service.AuditService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rw.ac.auca.attendance.repository.AttendanceRepository;
import rw.ac.auca.exception.BusinessRuleException;
import rw.ac.auca.exception.ResourceNotFoundException;
import rw.ac.auca.session.domain.Session;
import rw.ac.auca.session.repository.SessionRepository;
import rw.ac.auca.course.domain.Course;
import rw.ac.auca.course.service.CourseService;
import rw.ac.auca.user.domain.User;
import rw.ac.auca.user.service.CurrentUserService;

import java.util.List;
import java.util.UUID;

/**
 * The Class SessionServiceImpl.
 *
 * @author Ishimwe Mireille
 * @version 1.0
 */
@Service
@Transactional
public class SessionServiceImpl implements SessionService {

    @Autowired
    private SessionRepository sessionRepository;

    @Autowired
    private AuditService auditService;

    @Autowired
    private AttendanceRepository attendanceRepository;

    @Autowired
    private CourseService courseService;

    @Autowired
    private CurrentUserService currentUserService;

    @Override
    public Session createSession(SessionRequest request) {
        checkTimes(request);
        if (sessionRepository.existsBySessionDateAndStartTimeAndRoomIgnoreCase(
                request.getSessionDate(), request.getStartTime(), request.getRoom())) {
            throw new BusinessRuleException("Room " + request.getRoom()
                    + " is already booked on " + request.getSessionDate()
                    + " at " + request.getStartTime() + ".");
        }
        Session session = new Session();
        apply(session, request);
        Session saved = sessionRepository.save(session);
        auditService.record("Session", saved.getId(), AuditService.CREATE, null, saved);
        return saved;
    }

    @Override
    public Session updateSession(UUID id, SessionRequest request) {
        Session found = findSessionById(id);
        checkTimes(request);

        if (sessionRepository.existsBySessionDateAndStartTimeAndRoomIgnoreCaseAndIdNot(
                request.getSessionDate(), request.getStartTime(), request.getRoom(), id)) {
            throw new BusinessRuleException("Room " + request.getRoom()
                    + " is already booked on " + request.getSessionDate()
                    + " at " + request.getStartTime() + ".");
        }

        long marked = attendanceRepository.countBySessionId(id);
        if (marked > request.getCapacity()) {
            throw new BusinessRuleException("Capacity cannot be reduced to "
                    + request.getCapacity() + " because " + marked
                    + " student(s) are already marked for this session.");
        }

        apply(found, request);
        Session saved = sessionRepository.save(found);
        auditService.record("Session", saved.getId(), AuditService.UPDATE, null, saved);
        return saved;
    }

    private void apply(Session session, SessionRequest request) {
        Course course = courseService.findCourseById(request.getCourseId());

        if (!course.isActive()) {
            throw new BusinessRuleException("\"" + course.getTitle()
                    + "\" is not an active course, so no session can be scheduled in it.");
        }
        if (request.getSessionDate().isBefore(course.getStartDate())
                || request.getSessionDate().isAfter(course.getEndDate())) {
            throw new BusinessRuleException("A session on " + request.getSessionDate()
                    + " falls outside \"" + course.getTitle() + "\", which runs from "
                    + course.getStartDate() + " to " + course.getEndDate() + ".");
        }

        session.setTitle(request.getTitle());
        session.setSessionDate(request.getSessionDate());
        session.setStartTime(request.getStartTime());
        session.setEndTime(request.getEndTime());
        session.setRoom(request.getRoom());
        session.setCourse(course);
        session.setCapacity(request.getCapacity());
        session.setActive(request.isActive());
    }

    private void checkTimes(SessionRequest request) {
        if (!request.getEndTime().isAfter(request.getStartTime())) {
            throw new BusinessRuleException("End time must be after the start time.");
        }
    }

    @Override
    public void deleteSession(UUID id) {
        Session found = findSessionById(id);
        long marked = attendanceRepository.countBySessionId(id);
        if (marked > 0) {
            throw new BusinessRuleException("Cannot delete this session because "
                    + marked + " attendance record(s) exist for it.");
        }
        auditService.record("Session", found.getId(), AuditService.DELETE, found, null);
        sessionRepository.delete(found);
    }

    @Override
    @Transactional(readOnly = true)
    public Session findSessionById(UUID id) {
        Session found = sessionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("No session found with id " + id));
        assertVisible(found);
        return found;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Session> findAllSessions() {
        // FR-6: a trainer sees only the sessions they teach.
        if (currentUserService.isTrainerOnly()) {
            return findByTrainer(currentUserService.require().getId());
        }
        return sessionRepository.findAllByOrderBySessionDateDescStartTimeAsc();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Session> findByTrainer(UUID trainerId) {
        return sessionRepository.findByCourseTrainerIdOrderBySessionDateDescStartTimeAsc(trainerId);
    }

    /** Throws when a trainer-only caller asks for a session that is not theirs. */
    private void assertVisible(Session session) {
        if (!currentUserService.isTrainerOnly()) {
            return;
        }
        User caller = currentUserService.require();
        if (!session.getCourse().getTrainer().getId().equals(caller.getId())) {
            throw new AccessDeniedException(
                    "You may only view sessions on courses you lead.");
        }
    }
}
