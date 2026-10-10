package rw.ac.auca.notification.domain;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * The Class NotificationLog. A MongoDB document, not a table.
 *
 * It is append-only, its payload has a different shape for every channel, and
 * it is never joined to an attendance row in a query, so a document store fits
 * it better than a relational table would. See section 8.3 of the Part 1
 * document for the full argument.
 *
 * @author Ishimwe Mireille
 * @version 1.0
 */
@Getter
@Setter
@Document(collection = "notification_log")
public class NotificationLog {

    @Id
    private String id;

    /** The broker message id. Unique, so a redelivered message is not logged twice. */
    @Indexed(unique = true)
    private String eventId;

    /** ABSENCE_ALERT or WEEKLY_DIGEST. */
    private String type;

    private NotificationChannel channel;

    /** { studentId, name, email } for EMAIL; { studentId, name, phone } for SMS. */
    private Map<String, Object> recipient;

    /** { sessionId, sessionTitle, sessionDate, status } */
    private Map<String, Object> context;

    /** { subject, body } for EMAIL; { text } for SMS. The shape differs by channel. */
    private Map<String, Object> payload;

    /** One entry per delivery attempt; grows on every retry. */
    private List<Map<String, Object>> attempts = new ArrayList<>();

    private NotificationStatus status = NotificationStatus.PENDING;

    private LocalDateTime createdAt = LocalDateTime.now();
}
