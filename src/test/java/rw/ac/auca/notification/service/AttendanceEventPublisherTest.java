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
import rw.ac.auca.attendance.domain.Attendance;
import rw.ac.auca.attendance.domain.AttendanceStatus;
import rw.ac.auca.config.RabbitConfig;
import rw.ac.auca.outbox.domain.OutboxEvent;
import rw.ac.auca.outbox.domain.OutboxStatus;
import rw.ac.auca.outbox.repository.OutboxEventRepository;
import rw.ac.auca.session.domain.Session;
import rw.ac.auca.student.domain.Student;
import rw.ac.auca.user.domain.User;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * The Class AttendanceEventPublisherTest.
 *
 * The publisher no longer speaks to the broker at all: it writes a row that
 * joins the attendance transaction. That is what makes an alert impossible to
 * lose, so these tests pin the row rather than a message.
 *
 * @author Ishimwe Mireille
 * @version 1.0
 */
@ExtendWith(MockitoExtension.class)
class AttendanceEventPublisherTest {

    @Mock
    private OutboxEventRepository outboxEventRepository;

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

        User actor = new User();
        actor.setId(UUID.randomUUID());
        actor.setEmail("cohort.leader@auca.ac.rw");

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
        record.setRecordedBy(actor);
        record.setStatus(AttendanceStatus.ABSENT);
        record.setRemarks("Did not attend the session");
    }

    @Test
    @DisplayName("writes an absence to the outbox, addressed to the right exchange")
    void writesAnAbsenceToTheOutbox() {
        publisher.publishIfAbsent(record);

        ArgumentCaptor<OutboxEvent> captor = ArgumentCaptor.forClass(OutboxEvent.class);
        verify(outboxEventRepository).save(captor.capture());

        OutboxEvent saved = captor.getValue();
        assertThat(saved.getExchange()).isEqualTo(RabbitConfig.EXCHANGE);
        assertThat(saved.getRoutingKey()).isEqualTo(RabbitConfig.ROUTING_ABSENT);
        assertThat(saved.getStatus()).isEqualTo(OutboxStatus.PENDING);
        assertThat(saved.getAttempts()).isZero();
        assertThat(saved.getPayload())
                .contains("ABSENCE_ALERT")
                .contains("aline.24011@auca.ac.rw")
                .contains("0788123456")
                .contains("Web Technology")
                .contains("eventId");
    }

    @Test
    @DisplayName("writes nothing for a student who was there")
    void writesNothingWhenPresent() {
        record.setStatus(AttendanceStatus.PRESENT);
        record.setTimeIn(LocalTime.of(7, 56));

        publisher.publishIfAbsent(record);

        verify(outboxEventRepository, never()).save(any());
    }

    @Test
    @DisplayName("writes nothing for a student who was excused")
    void writesNothingWhenExcused() {
        record.setStatus(AttendanceStatus.EXCUSED);

        publisher.publishIfAbsent(record);

        verify(outboxEventRepository, never()).save(any());
    }
}
