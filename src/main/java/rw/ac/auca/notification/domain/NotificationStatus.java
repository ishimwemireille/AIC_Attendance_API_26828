package rw.ac.auca.notification.domain;

/**
 * The Enum NotificationStatus.
 *
 * @author Ishimwe Mireille
 * @version 1.0
 */
public enum NotificationStatus {
    PENDING,
    SENT,
    FAILED,

    /** The student has no address or number on this channel, so nothing was sent. */
    SKIPPED
}
