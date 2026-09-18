package rw.ac.auca.session.domain;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Check;
import rw.ac.auca.base.BaseEntity;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * The Class Session.
 *
 * @author Ishimwe Mireille
 * @version 1.0
 */
@Getter
@Setter
@Entity
@Table(name = "sessions",
        uniqueConstraints = @UniqueConstraint(name = "uk_session_date_time_room",
                columnNames = {"session_date", "start_time", "room"}))
@Check(constraints = "end_time > start_time AND capacity >= 1")
public class Session extends BaseEntity {

    @NotBlank(message = "Title is required")
    @Size(min = 3, max = 80, message = "Title must be between 3 and 80 characters")
    @Column(name = "title", nullable = false, length = 80)
    private String title;

    @NotNull(message = "Session date is required")
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Column(name = "session_date", nullable = false)
    private LocalDate sessionDate;

    @NotNull(message = "Start time is required")
    @JsonFormat(pattern = "HH:mm")
    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @NotNull(message = "End time is required")
    @JsonFormat(pattern = "HH:mm")
    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    @NotBlank(message = "Room is required")
    @Size(min = 1, max = 20, message = "Room must be between 1 and 20 characters")
    @Column(name = "room", nullable = false, length = 20)
    private String room;

    @NotBlank(message = "Trainer is required")
    @Size(min = 3, max = 60, message = "Trainer must be between 3 and 60 characters")
    @Column(name = "trainer", nullable = false, length = 60)
    private String trainer;

    @Min(value = 1, message = "Capacity must be at least 1")
    @Column(name = "capacity", nullable = false)
    private int capacity;

    @Column(name = "active", nullable = false)
    private boolean active = Boolean.TRUE;
}
