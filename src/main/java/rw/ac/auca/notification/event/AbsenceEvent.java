package rw.ac.auca.notification.event;

import lombok.Getter;
import lombok.Setter;

/**
 * The Class AbsenceEvent. Published when a student is marked ABSENT.
 *
 * It carries everything a consumer needs, so the consumer never has to read
 * the relational database back.
 *
 * @author Ishimwe Mireille
 * @version 1.0
 */
@Getter
@Setter
public class AbsenceEvent {

    /** Unique per publish; the consumer uses it so a redelivery is not logged twice. */
    private String eventId;

    private String type;

    private String studentId;
    private String studentName;
    private String studentEmail;
    private String studentPhone;

    private String sessionId;
    private String sessionTitle;
    private String sessionDate;

    private String status;
    private String remarks;
    private String recordedBy;
}
