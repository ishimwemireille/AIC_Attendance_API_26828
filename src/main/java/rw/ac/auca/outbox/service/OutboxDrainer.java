package rw.ac.auca.outbox.service;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rw.ac.auca.outbox.domain.OutboxEvent;
import rw.ac.auca.outbox.domain.OutboxStatus;
import rw.ac.auca.outbox.repository.OutboxEventRepository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * The Class OutboxDrainer.
 *
 * Publishes whatever is waiting in the outbox and marks it sent. Runs on a
 * short interval so an alert still reaches the student within seconds, and
 * simply finds the same rows still waiting when the broker comes back after an
 * outage - which is what makes delivery survive the broker being down.
 *
 * @author Ishimwe Mireille
 * @version 1.0
 */
@Service
public class OutboxDrainer {

    @Autowired
    private OutboxEventRepository outboxEventRepository;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Value("${app.outbox.max-attempts:10}")
    private int maxAttempts;

    @Scheduled(fixedDelayString = "${app.outbox.poll-interval-ms:1000}")
    @Transactional
    public void drain() {
        List<OutboxEvent> waiting =
                outboxEventRepository.findTop100ByStatusOrderByCreatedOnAsc(OutboxStatus.PENDING);

        for (OutboxEvent event : waiting) {
            try {
                rabbitTemplate.convertAndSend(
                        event.getExchange(), event.getRoutingKey(), event.getPayload());
                event.setStatus(OutboxStatus.SENT);
                event.setSentOn(LocalDateTime.now());
                event.setLastError(null);
            } catch (Exception exception) {
                event.setAttempts(event.getAttempts() + 1);
                event.setLastError(trim(exception.getMessage()));
                if (event.getAttempts() >= maxAttempts) {
                    // Keep it rather than retry for ever; it stays visible for inspection.
                    event.setStatus(OutboxStatus.FAILED);
                }
                // The broker is almost certainly down for the rest of this batch too.
                outboxEventRepository.save(event);
                break;
            }
            outboxEventRepository.save(event);
        }
    }

    private String trim(String message) {
        if (message == null) {
            return null;
        }
        return message.length() > 300 ? message.substring(0, 300) : message;
    }
}
