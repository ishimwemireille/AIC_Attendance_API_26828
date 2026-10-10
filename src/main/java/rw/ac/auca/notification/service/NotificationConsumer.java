package rw.ac.auca.notification.service;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import rw.ac.auca.config.RabbitConfig;
import rw.ac.auca.notification.domain.NotificationChannel;
import rw.ac.auca.notification.domain.NotificationLog;
import rw.ac.auca.notification.domain.NotificationStatus;
import rw.ac.auca.notification.event.AbsenceEvent;
import rw.ac.auca.notification.repository.NotificationLogRepository;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The Class NotificationConsumer. Reads the absence events and records the
 * delivery attempt in MongoDB.
 *
 * Actually handing the message to an email or SMS provider is a one-line change
 * in deliver(); everything around it - the queue, the retry, the log - is real.
 *
 * @author Ishimwe Mireille
 * @version 1.0
 */
@Service
public class NotificationConsumer {

    @Autowired
    private NotificationLogRepository notificationLogRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @RabbitListener(queues = RabbitConfig.QUEUE_EMAIL)
    public void onEmail(String message) {
        handle(message, NotificationChannel.EMAIL);
    }

    @RabbitListener(queues = RabbitConfig.QUEUE_SMS)
    public void onSms(String message) {
        handle(message, NotificationChannel.SMS);
    }

    private void handle(String message, NotificationChannel channel) {
        AbsenceEvent event = objectMapper.readValue(message, AbsenceEvent.class);

        // The same event reaches two queues, so the key must include the channel.
        String key = event.getEventId() + ":" + channel;
        if (notificationLogRepository.existsByEventId(key)) {
            return;
        }

        NotificationLog log = new NotificationLog();
        log.setEventId(key);
        log.setType(event.getType());
        log.setChannel(channel);
        log.setRecipient(recipient(event, channel));
        log.setContext(context(event));
        log.setPayload(payload(event, channel));

        Map<String, Object> attempt = new LinkedHashMap<>();
        attempt.put("at", LocalDateTime.now().toString());
        try {
            String providerResponse = deliver(event, channel);
            attempt.put("outcome", "SENT");
            attempt.put("providerResponse", providerResponse);
            log.setStatus(NotificationStatus.SENT);
        } catch (Exception exception) {
            attempt.put("outcome", "FAILED");
            attempt.put("providerResponse", String.valueOf(exception.getMessage()));
            log.setStatus(NotificationStatus.FAILED);
        }
        log.getAttempts().add(attempt);
        notificationLogRepository.save(log);
    }

    /** Where a real provider would be called. */
    private String deliver(AbsenceEvent event, NotificationChannel channel) {
        String target = channel == NotificationChannel.EMAIL
                ? event.getStudentEmail() : event.getStudentPhone();
        System.out.println("[" + channel + "] to " + target
                + " - absent from " + event.getSessionTitle() + " on " + event.getSessionDate());
        return "accepted by the " + channel.name().toLowerCase() + " gateway (simulated)";
    }

    private Map<String, Object> recipient(AbsenceEvent event, NotificationChannel channel) {
        Map<String, Object> recipient = new LinkedHashMap<>();
        recipient.put("studentId", event.getStudentId());
        recipient.put("name", event.getStudentName());
        if (channel == NotificationChannel.EMAIL) {
            recipient.put("email", event.getStudentEmail());
        } else {
            recipient.put("phone", event.getStudentPhone());
        }
        return recipient;
    }

    private Map<String, Object> context(AbsenceEvent event) {
        Map<String, Object> context = new LinkedHashMap<>();
        context.put("sessionId", event.getSessionId());
        context.put("sessionTitle", event.getSessionTitle());
        context.put("sessionDate", event.getSessionDate());
        context.put("status", event.getStatus());
        context.put("recordedBy", event.getRecordedBy());
        return context;
    }

    /** The shape differs by channel; this is why the log is a document, not a row. */
    private Map<String, Object> payload(AbsenceEvent event, NotificationChannel channel) {
        Map<String, Object> payload = new LinkedHashMap<>();
        if (channel == NotificationChannel.EMAIL) {
            payload.put("subject", "You were marked absent from " + event.getSessionTitle());
            payload.put("body", "Dear " + event.getStudentName() + ",\n\n"
                    + "Our records show you were absent from " + event.getSessionTitle()
                    + " on " + event.getSessionDate() + ".\n"
                    + "If this is a mistake, tell your cohort leader today.\n\n"
                    + "AIC Attendance, AUCA");
        } else {
            payload.put("text", "AUCA AIC: you were marked absent from "
                    + event.getSessionTitle() + " on " + event.getSessionDate()
                    + ". Contact your cohort leader if this is wrong.");
        }
        return payload;
    }
}
