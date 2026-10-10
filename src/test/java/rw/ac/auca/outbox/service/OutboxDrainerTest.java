package rw.ac.auca.outbox.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.AmqpConnectException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import rw.ac.auca.outbox.domain.OutboxEvent;
import rw.ac.auca.outbox.domain.OutboxStatus;
import rw.ac.auca.outbox.repository.OutboxEventRepository;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * The Class OutboxDrainerTest.
 *
 * The reliability promise is that an alert survives the broker being down. The
 * drainer is where that is kept: a failed send must leave the row waiting, not
 * discard it.
 *
 * @author Ishimwe Mireille
 * @version 1.0
 */
@ExtendWith(MockitoExtension.class)
class OutboxDrainerTest {

    @Mock
    private OutboxEventRepository outboxEventRepository;

    @Mock
    private RabbitTemplate rabbitTemplate;

    @InjectMocks
    private OutboxDrainer drainer;

    private OutboxEvent event;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(drainer, "maxAttempts", 10);
        event = new OutboxEvent();
        event.setExchange("attendance.events");
        event.setRoutingKey("attendance.absent");
        event.setPayload("{\"type\":\"ABSENCE_ALERT\"}");
    }

    @Test
    @DisplayName("publishes what is waiting and marks it sent")
    void publishesAndMarksSent() {
        when(outboxEventRepository.findTop100ByStatusOrderByCreatedOnAsc(OutboxStatus.PENDING))
                .thenReturn(List.of(event));

        drainer.drain();

        verify(rabbitTemplate).convertAndSend(
                "attendance.events", "attendance.absent", "{\"type\":\"ABSENCE_ALERT\"}");
        assertThat(event.getStatus()).isEqualTo(OutboxStatus.SENT);
        assertThat(event.getSentOn()).isNotNull();
    }

    @Test
    @DisplayName("a broker that is down leaves the event waiting rather than losing it")
    void keepsTheEventWhenTheBrokerIsDown() {
        when(outboxEventRepository.findTop100ByStatusOrderByCreatedOnAsc(OutboxStatus.PENDING))
                .thenReturn(List.of(event));
        doThrow(new AmqpConnectException(new RuntimeException("connection refused")))
                .when(rabbitTemplate).convertAndSend(anyString(), anyString(), anyString());

        assertThatCode(() -> drainer.drain()).doesNotThrowAnyException();

        assertThat(event.getStatus()).isEqualTo(OutboxStatus.PENDING);
        assertThat(event.getAttempts()).isEqualTo(1);
        assertThat(event.getLastError()).contains("connection refused");
        assertThat(event.getSentOn()).isNull();
    }

    @Test
    @DisplayName("stops the batch on the first failure instead of hammering a dead broker")
    void stopsTheBatchOnFailure() {
        OutboxEvent second = new OutboxEvent();
        second.setExchange("attendance.events");
        second.setRoutingKey("attendance.absent");
        second.setPayload("{}");
        when(outboxEventRepository.findTop100ByStatusOrderByCreatedOnAsc(OutboxStatus.PENDING))
                .thenReturn(List.of(event, second));
        doThrow(new AmqpConnectException(new RuntimeException("connection refused")))
                .when(rabbitTemplate).convertAndSend(anyString(), anyString(), anyString());

        drainer.drain();

        verify(rabbitTemplate, times(1)).convertAndSend(anyString(), anyString(), anyString());
        assertThat(second.getAttempts()).isZero();
    }

    @Test
    @DisplayName("gives up after the maximum number of attempts, keeping the row for inspection")
    void givesUpAfterTooManyAttempts() {
        ReflectionTestUtils.setField(drainer, "maxAttempts", 3);
        event.setAttempts(2);
        when(outboxEventRepository.findTop100ByStatusOrderByCreatedOnAsc(OutboxStatus.PENDING))
                .thenReturn(List.of(event));
        doThrow(new AmqpConnectException(new RuntimeException("connection refused")))
                .when(rabbitTemplate).convertAndSend(anyString(), anyString(), anyString());

        drainer.drain();

        assertThat(event.getAttempts()).isEqualTo(3);
        assertThat(event.getStatus()).isEqualTo(OutboxStatus.FAILED);
    }
}
