package rw.ac.auca.notification.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.AmqpConnectException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import rw.ac.auca.attendance.domain.Attendance;
import rw.ac.auca.attendance.domain.AttendanceStatus;
import rw.ac.auca.config.RabbitConfig;
import rw.ac.auca.session.domain.Session;
import rw.ac.auca.student.domain.Student;
import rw.ac.auca.user.domain.User;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * The Class AttendanceEventPublisherTest.
 *
 * The reliability claim in the report is that a broker which is down cannot
 * undo an attendance write. That is the easiest promise in the project to break
 * by accident later - removing one try/catch would do it - so it is pinned
 * here rather than left to the manual test that first demonstrated it.
 *
 * @author Ishimwe Mireille
 * @version 1.0
 */
@ExtendWith(MockitoExtension.class)
class AttendanceEventPublisherTest {

    @Mock
    private RabbitTemplate rabbitTemplate;

    @Spy
    private ObjectMapper objectMapper = JsonMapper.builder().build();

    @InjectMocks
    private AttendanceEventPublisher publisher;

    private Attendance record;

    @BeforeEach
    void setUp() {
        Student student = new Student();
        student.setId(UUID.randomUUID());
        student.setFirstName("Aline");
        student.setLastName("Uwase");
        student.setEmail("aline.24011@auca.ac.rw");
        student.setPhone("0788123456");

        User trainer = new User();
        trainer.setId(UUID.randomUUID());
        trainer.setEmail("cohort.leader@auca.ac.rw");

        Session session = new Session();
        session.setId(UUID.randomUUID());
        session.setTitle("Web Technology");
        session.setSessionDate(LocalDate.of(2026, 10, 10));
        session.setStartTime(LocalTime.of(8, 0));
        session.setEndTime(LocalTime.of(10, 0));

        record = new Attendance();
        record.setId(UUID.randomUUID());
        record.setStudent(student);
        record.setSession(session);
        record.setRecordedBy(trainer);
        record.setStatus(AttendanceStatus.ABSENT);
        record.setRemarks("Did not attend the session");
    }

    @Test
    @DisplayName("publishes an absence to the right exchange and routing key")
    void publishesAnAbsence() {
        publisher.publishIfAbsent(record);

        ArgumentCaptor<String> body = ArgumentCaptor.forClass(String.class);
        verify(rabbitTemplate).convertAndSend(
                eq(RabbitConfig.EXCHANGE), eq(RabbitConfig.ROUTING_ABSENT), body.capture());

        assertThat(body.getValue())
                .contains("ABSENCE_ALERT")
                .contains("aline.24011@auca.ac.rw")
                .contains("0788123456")
                .contains("Web Technology")
                .contains("eventId");
    }

    @Test
    @DisplayName("publishes nothing for a student who was there")
    void publishesNothingWhenPresent() {
        record.setStatus(AttendanceStatus.PRESENT);
        record.setTimeIn(LocalTime.of(7, 56));

        publisher.publishIfAbsent(record);

        verify(rabbitTemplate, never()).convertAndSend(anyString(), anyString(), any(Object.class));
    }

    @Test
    @DisplayName("publishes nothing for a student who was excused")
    void publishesNothingWhenExcused() {
        record.setStatus(AttendanceStatus.EXCUSED);

        publisher.publishIfAbsent(record);

        verify(rabbitTemplate, never()).convertAndSend(anyString(), anyString(), any(Object.class));
    }

    @Test
    @DisplayName("a broker that is down does not fail the attendance write")
    void brokerFailureDoesNotPropagate() {
        doThrow(new AmqpConnectException(new RuntimeException("connection refused")))
                .when(rabbitTemplate).convertAndSend(anyString(), anyString(), any(Object.class));

        assertThatCode(() -> publisher.publishIfAbsent(record)).doesNotThrowAnyException();
    }
}
