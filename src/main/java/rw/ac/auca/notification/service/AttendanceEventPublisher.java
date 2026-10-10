package rw.ac.auca.notification.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import rw.ac.auca.attendance.domain.Attendance;
import rw.ac.auca.attendance.domain.AttendanceStatus;
import rw.ac.auca.config.RabbitConfig;
import rw.ac.auca.notification.event.AbsenceEvent;
import rw.ac.auca.outbox.domain.OutboxEvent;
import rw.ac.auca.outbox.repository.OutboxEventRepository;
import tools.jackson.databind.ObjectMapper;

import java.util.UUID;

/**
 * The Class AttendanceEventPublisher.
 *
 * US-3: marking a student ABSENT must tell them the same day. This writes the
 * event to the outbox table rather than sending it to the broker, so it joins
 * the same transaction as the attendance record: either both are committed or
 * neither is. OutboxDrainer does the sending.
 *
 * The earlier version called the broker directly inside a try/catch, which kept
 * the attendance record safe but dropped the alert when the broker was down.
 * This closes that.
 *
 * @author Ishimwe Mireille
 * @version 1.0
 */
@Service
public class AttendanceEventPublisher {

    @Autowired
    private OutboxEventRepository outboxEventRepository;

    @Autowired
    private ObjectMapper objectMapper;

    public void publishIfAbsent(Attendance attendance) {
        if (attendance.getStatus() != AttendanceStatus.ABSENT) {
            return;
        }

        AbsenceEvent event = new AbsenceEvent();
        event.setEventId(UUID.randomUUID().toString());
        event.setType("ABSENCE_ALERT");
        event.setStudentId(attendance.getStudent().getId().toString());
        event.setStudentName(attendance.getStudent().getFullName());
        event.setStudentEmail(attendance.getStudent().getEmail());
        event.setStudentPhone(attendance.getStudent().getPhone());
        event.setSessionId(attendance.getSession().getId().toString());
        event.setSessionTitle(attendance.getSession().getTitle());
        event.setSessionDate(String.valueOf(attendance.getSession().getSessionDate()));
        event.setStatus(attendance.getStatus().name());
        event.setRemarks(attendance.getRemarks());
        event.setRecordedBy(attendance.getRecordedBy().getEmail());

        OutboxEvent row = new OutboxEvent();
        row.setExchange(RabbitConfig.EXCHANGE);
        row.setRoutingKey(RabbitConfig.ROUTING_ABSENT);
        row.setPayload(objectMapper.writeValueAsString(event));
        outboxEventRepository.save(row);
    }
}
