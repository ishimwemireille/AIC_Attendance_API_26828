package rw.ac.auca.attendance.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import rw.ac.auca.attendance.domain.Attendance;
import rw.ac.auca.attendance.domain.AttendanceStatus;
import rw.ac.auca.attendance.repository.AttendanceRepository;
import rw.ac.auca.audit.service.AuditService;
import rw.ac.auca.exception.BusinessRuleException;
import rw.ac.auca.notification.service.AttendanceEventPublisher;
import rw.ac.auca.session.domain.Session;
import rw.ac.auca.session.service.SessionService;
import rw.ac.auca.student.domain.Student;
import rw.ac.auca.student.service.StudentService;
import rw.ac.auca.user.domain.User;
import rw.ac.auca.user.service.CurrentUserService;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * The Class AttendanceServiceImplTest.
 *
 * Covers the business rules that decide whether a record may be written. These
 * are the densest logic in the project and have no external dependencies, so
 * they are tested in isolation with mocks rather than against a database.
 *
 * @author Ishimwe Mireille
 * @version 1.0
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AttendanceServiceImplTest {

    @Mock
    private AttendanceRepository attendanceRepository;
    @Mock
    private StudentService studentService;
    @Mock
    private SessionService sessionService;
    @Mock
    private CurrentUserService currentUserService;
    @Mock
    private AuditService auditService;
    @Mock
    private AttendanceEventPublisher eventPublisher;

    @InjectMocks
    private AttendanceServiceImpl service;

    private Student student;
    private Session session;
    private User actor;

    @BeforeEach
    void setUp() {
        student = new Student();
        student.setId(UUID.randomUUID());
        student.setRegNumber("24011");
        student.setFirstName("Aline");
        student.setLastName("Uwase");
        student.setActive(true);

        session = new Session();
        session.setId(UUID.randomUUID());
        session.setTitle("Web Technology");
        session.setSessionDate(LocalDate.now());
        session.setStartTime(LocalTime.of(8, 0));
        session.setEndTime(LocalTime.of(10, 0));
        session.setCapacity(40);
        session.setActive(true);

        actor = new User();
        actor.setId(UUID.randomUUID());
        actor.setEmail("cohort.leader@auca.ac.rw");
        actor.setFullName("Mireille Ishimwe");

        when(studentService.findStudentById(any())).thenReturn(student);
        when(sessionService.findSessionById(any())).thenReturn(session);
        when(currentUserService.require()).thenReturn(actor);
        when(attendanceRepository.existsByStudentIdAndSessionId(any(), any())).thenReturn(false);
        when(attendanceRepository.countBySessionId(any())).thenReturn(0L);
        when(attendanceRepository.save(any(Attendance.class)))
                .thenAnswer(invocation -> {
                    Attendance saved = invocation.getArgument(0);
                    saved.setId(UUID.randomUUID());
                    return saved;
                });
    }

    private AttendanceRequest request(AttendanceStatus status, LocalTime timeIn, String remarks) {
        AttendanceRequest request = new AttendanceRequest();
        request.setStudentId(student.getId());
        request.setSessionId(session.getId());
        request.setStatus(status);
        request.setTimeIn(timeIn);
        request.setRemarks(remarks);
        return request;
    }

    // ------------------------------------------------------------ happy path

    @Test
    @DisplayName("saves a valid record and stamps the member of staff who took it")
    void savesValidRecordAndStampsActor() {
        Attendance saved = service.markAttendance(
                request(AttendanceStatus.PRESENT, LocalTime.of(7, 56), null));

        assertThat(saved.getStatus()).isEqualTo(AttendanceStatus.PRESENT);
        assertThat(saved.getTimeIn()).isEqualTo(LocalTime.of(7, 56));
        assertThat(saved.getRecordedBy()).isSameAs(actor);
        assertThat(saved.getStudent()).isSameAs(student);
        verify(auditService).record(anyString(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("does not keep a time in for a student who was absent")
    void clearsTimeInWhenAbsent() {
        Attendance saved = service.markAttendance(
                request(AttendanceStatus.ABSENT, LocalTime.of(8, 0), "Did not attend"));

        assertThat(saved.getTimeIn()).isNull();
    }

    // -------------------------------------------------------------- US-3

    @Test
    @DisplayName("publishes an absence event when the status is ABSENT")
    void publishesWhenAbsent() {
        Attendance saved = service.markAttendance(
                request(AttendanceStatus.ABSENT, null, "Did not attend"));

        ArgumentCaptor<Attendance> captor = ArgumentCaptor.forClass(Attendance.class);
        verify(eventPublisher).publishIfAbsent(captor.capture());
        assertThat(captor.getValue()).isSameAs(saved);
    }

    // --------------------------------------------------------- US-1, the rules

    @Test
    @DisplayName("refuses a second record for the same student and session")
    void refusesDuplicate() {
        when(attendanceRepository.existsByStudentIdAndSessionId(any(), any())).thenReturn(true);

        assertThatThrownBy(() -> service.markAttendance(
                request(AttendanceStatus.PRESENT, LocalTime.of(7, 56), null)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("already marked");

        verify(attendanceRepository, never()).save(any());
    }

    @Test
    @DisplayName("refuses a student who is not active")
    void refusesInactiveStudent() {
        student.setActive(false);

        assertThatThrownBy(() -> service.markAttendance(
                request(AttendanceStatus.PRESENT, LocalTime.of(7, 56), null)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("not an active student");
    }

    @Test
    @DisplayName("refuses a session that has been cancelled")
    void refusesCancelledSession() {
        session.setActive(false);

        assertThatThrownBy(() -> service.markAttendance(
                request(AttendanceStatus.PRESENT, LocalTime.of(7, 56), null)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("cancelled");
    }

    @Test
    @DisplayName("refuses a session that has not happened yet")
    void refusesFutureSession() {
        session.setSessionDate(LocalDate.now().plusDays(1));

        assertThatThrownBy(() -> service.markAttendance(
                request(AttendanceStatus.PRESENT, LocalTime.of(7, 56), null)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("in the future");
    }

    @Test
    @DisplayName("refuses PRESENT without a time in")
    void refusesPresentWithoutTimeIn() {
        assertThatThrownBy(() -> service.markAttendance(
                request(AttendanceStatus.PRESENT, null, null)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Time in is required");
    }

    @Test
    @DisplayName("refuses an arrival after the session had ended")
    void refusesArrivalAfterTheEnd() {
        assertThatThrownBy(() -> service.markAttendance(
                request(AttendanceStatus.LATE, LocalTime.of(10, 30), null)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("after the session ended");
    }

    @Test
    @DisplayName("refuses PRESENT when the student arrived after the start, and says to use LATE")
    void refusesPresentWhenLate() {
        assertThatThrownBy(() -> service.markAttendance(
                request(AttendanceStatus.PRESENT, LocalTime.of(8, 20), null)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Use LATE");
    }

    @Test
    @DisplayName("refuses LATE when the student arrived on time, and says to use PRESENT")
    void refusesLateWhenOnTime() {
        assertThatThrownBy(() -> service.markAttendance(
                request(AttendanceStatus.LATE, LocalTime.of(7, 55), null)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Use PRESENT");
    }

    @Test
    @DisplayName("refuses ABSENT without a reason")
    void refusesAbsentWithoutReason() {
        assertThatThrownBy(() -> service.markAttendance(
                request(AttendanceStatus.ABSENT, null, "no")))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("reason of at least 5 characters");
    }

    @Test
    @DisplayName("refuses EXCUSED without a reason")
    void refusesExcusedWithoutReason() {
        assertThatThrownBy(() -> service.markAttendance(
                request(AttendanceStatus.EXCUSED, null, null)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("reason of at least 5 characters");
    }

    @Test
    @DisplayName("refuses to mark a student when the session is already full")
    void refusesWhenSessionIsFull() {
        session.setCapacity(2);
        when(attendanceRepository.countBySessionId(any())).thenReturn(2L);

        assertThatThrownBy(() -> service.markAttendance(
                request(AttendanceStatus.PRESENT, LocalTime.of(7, 56), null)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("is full");
    }
}
