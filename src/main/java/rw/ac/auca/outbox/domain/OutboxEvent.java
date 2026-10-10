package rw.ac.auca.outbox.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import rw.ac.auca.base.BaseEntity;

import java.time.LocalDateTime;

/**
 * The Class OutboxEvent.
 *
 * A message waiting to be published, written in the same transaction as the
 * record that caused it. That is the whole point: either the attendance row and
 * its event are both committed or neither is, so an event can never be lost
 * because the broker happened to be down at that moment.
 *
 * @author Ishimwe Mireille
 * @version 1.0
 */
@Getter
@Setter
@Entity
@Table(name = "outbox_events",
        indexes = @Index(name = "idx_outbox_pending", columnList = "status, created_on"))
public class OutboxEvent extends BaseEntity {

    @NotBlank(message = "Exchange is required")
    @Column(name = "exchange_name", nullable = false, length = 60)
    private String exchange;

    @NotBlank(message = "Routing key is required")
    @Column(name = "routing_key", nullable = false, length = 60)
    private String routingKey;

    @NotBlank(message = "Payload is required")
    @Column(name = "payload", nullable = false, columnDefinition = "text")
    private String payload;

    @NotNull(message = "Status is required")
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "status", nullable = false, length = 10)
    private OutboxStatus status = OutboxStatus.PENDING;

    @Column(name = "attempts", nullable = false)
    private int attempts = 0;

    @Column(name = "last_error", length = 300)
    private String lastError;

    /** A timestamp, unlike BaseEntity's date, because ordering within a day matters. */
    @Column(name = "created_on", nullable = false)
    private LocalDateTime createdOn = LocalDateTime.now();

    @Column(name = "sent_on")
    private LocalDateTime sentOn;
}
