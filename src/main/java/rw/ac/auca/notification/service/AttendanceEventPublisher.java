package rw.ac.auca.notification.service;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import rw.ac.auca.attendance.domain.Attendance;
import rw.ac.auca.attendance.domain.AttendanceStatus;
import rw.ac.auca.config.RabbitConfig;
import rw.ac.auca.notification.event.AbsenceEvent;
import tools.jackson.databind.ObjectMapper;

import java.util.UUID;

/**
 * The Class AttendanceEventPublisher.
 *
 * US-3: marking a student ABSENT publishes an event so the student can be told
 * the same day. Publishing must never fail the attendance write, so a broker
 * that is down is logged and swallowed: the record is still saved.
 *
 * @author Ishimwe Mireille
 * @version 1.0
 */
@Service
public class AttendanceEventPublisher {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    public void publishIfAbsent(Attendance attendance) {
        if (attendance.getStatus() != AttendanceStatus.ABSENT) {
            return;
        }
        try {
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

            rabbitTemplate.convertAndSend(RabbitConfig.EXCHANGE, RabbitConfig.ROUTING_ABSENT,
                    objectMapper.writeValueAsString(event));
        } catch (Exception exception) {
            // The attendance record is already committed; losing the alert must not undo it.
            System.err.println("Absence event could not be published: " + exception.getMessage());
        }
    }
}
