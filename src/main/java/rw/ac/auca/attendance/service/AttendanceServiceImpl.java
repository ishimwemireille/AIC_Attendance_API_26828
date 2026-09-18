package rw.ac.auca.attendance.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rw.ac.auca.attendance.domain.Attendance;
import rw.ac.auca.attendance.domain.AttendanceStatus;
import rw.ac.auca.attendance.repository.AttendanceRepository;
import rw.ac.auca.exception.BusinessRuleException;
import rw.ac.auca.exception.ResourceNotFoundException;
import rw.ac.auca.session.domain.Session;
import rw.ac.auca.session.service.SessionService;
import rw.ac.auca.student.domain.Student;
import rw.ac.auca.student.service.StudentService;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * The Class AttendanceServiceImpl.
 *
 * @author Ishimwe Mireille
 * @version 1.0
 */
@Service
@Transactional
public class AttendanceServiceImpl implements AttendanceService {

    @Autowired
    private AttendanceRepository attendanceRepository;

    @Autowired
    private StudentService studentService;

    @Autowired
    private SessionService sessionService;

    @Override
    public Attendance markAttendance(AttendanceRequest request) {
        Student student = studentService.findStudentById(request.getStudentId());
        Session session = sessionService.findSessionById(request.getSessionId());

        if (attendanceRepository.existsByStudentIdAndSessionId(student.getId(), session.getId())) {
            throw new BusinessRuleException(student.getFullName()
                    + " is already marked for the session \"" + session.getTitle() + "\".");
        }
        checkBusinessRules(student, session, request);

        long marked = attendanceRepository.countBySessionId(session.getId());
        if (marked >= session.getCapacity()) {
            throw new BusinessRuleException("The session \"" + session.getTitle()
                    + "\" is full: capacity is " + session.getCapacity() + ".");
        }

        Attendance attendance = new Attendance();
        apply(attendance, student, session, request);
        return attendanceRepository.save(attendance);
    }

    @Override
    public Attendance updateAttendance(UUID id, AttendanceRequest request) {
        Attendance found = findAttendanceById(id);
        Student student = studentService.findStudentById(request.getStudentId());
        Session session = sessionService.findSessionById(request.getSessionId());

        if (attendanceRepository.existsByStudentIdAndSessionIdAndIdNot(
                student.getId(), session.getId(), id)) {
            throw new BusinessRuleException(student.getFullName()
                    + " is already marked for the session \"" + session.getTitle() + "\".");
        }
        checkBusinessRules(student, session, request);

        apply(found, student, session, request);
        return attendanceRepository.save(found);
    }

    private void checkBusinessRules(Student student, Session session, AttendanceRequest request) {
        if (!student.isActive()) {
            throw new BusinessRuleException(student.getFullName()
                    + " is not an active student and cannot be marked.");
        }
        if (!session.isActive()) {
            throw new BusinessRuleException("The session \"" + session.getTitle()
                    + "\" is cancelled and cannot be marked.");
        }
        if (session.getSessionDate().isAfter(LocalDate.now())) {
            throw new BusinessRuleException("Attendance cannot be recorded for \""
                    + session.getTitle() + "\" because it takes place on "
                    + session.getSessionDate() + ", which is in the future.");
        }

        AttendanceStatus status = request.getStatus();
        boolean attended = status == AttendanceStatus.PRESENT || status == AttendanceStatus.LATE;

        if (attended && request.getTimeIn() == null) {
            throw new BusinessRuleException("Time in is required when the status is " + status + ".");
        }
        if (attended && request.getTimeIn().isAfter(session.getEndTime())) {
            throw new BusinessRuleException("Time in " + request.getTimeIn()
                    + " is after the session ended at " + session.getEndTime() + ".");
        }
        if (status == AttendanceStatus.PRESENT
                && request.getTimeIn().isAfter(session.getStartTime())) {
            throw new BusinessRuleException("Arrival at " + request.getTimeIn()
                    + " is after the session started at " + session.getStartTime()
                    + ". Use LATE instead of PRESENT.");
        }
        if (status == AttendanceStatus.LATE
                && !request.getTimeIn().isAfter(session.getStartTime())) {
            throw new BusinessRuleException("Arrival at " + request.getTimeIn()
                    + " is not after the session start " + session.getStartTime()
                    + ". Use PRESENT instead of LATE.");
        }
        if ((status == AttendanceStatus.ABSENT || status == AttendanceStatus.EXCUSED)
                && (request.getRemarks() == null || request.getRemarks().trim().length() < 5)) {
            throw new BusinessRuleException(
                    "A reason of at least 5 characters is required when the status is " + status + ".");
        }
    }

    private void apply(Attendance attendance, Student student, Session session,
                       AttendanceRequest request) {
        AttendanceStatus status = request.getStatus();
        attendance.setStudent(student);
        attendance.setSession(session);
        attendance.setStatus(status);
        attendance.setRemarks(request.getRemarks());
        attendance.setTimeIn(
                status == AttendanceStatus.PRESENT || status == AttendanceStatus.LATE
                        ? request.getTimeIn() : null);
    }

    @Override
    public void deleteAttendance(UUID id) {
        attendanceRepository.delete(findAttendanceById(id));
    }

    @Override
    @Transactional(readOnly = true)
    public Attendance findAttendanceById(UUID id) {
        return attendanceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No attendance record found with id " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Attendance> findAllAttendances() {
        return attendanceRepository.findAllWithDetails();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Attendance> findBySession(UUID sessionId) {
        sessionService.findSessionById(sessionId);
        return attendanceRepository.findBySessionId(sessionId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Attendance> findByStudent(UUID studentId) {
        studentService.findStudentById(studentId);
        return attendanceRepository.findByStudentId(studentId);
    }
}
