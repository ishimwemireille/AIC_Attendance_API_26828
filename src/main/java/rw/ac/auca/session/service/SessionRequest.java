package rw.ac.auca.session.service;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

/**
 * The Class SessionRequest. What the client sends: the trainer is an id now,
 * not a name, because a trainer must be a real user for FR-6 to be enforceable.
 *
 * @author Ishimwe Mireille
 * @version 1.0
 */
@Getter
@Setter
public class SessionRequest {

    @NotBlank(message = "Title is required")
    @Size(min = 3, max = 80, message = "Title must be between 3 and 80 characters")
    private String title;

    @NotNull(message = "Session date is required")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate sessionDate;

    @NotNull(message = "Start time is required")
    @JsonFormat(pattern = "HH:mm")
    private LocalTime startTime;

    @NotNull(message = "End time is required")
    @JsonFormat(pattern = "HH:mm")
    private LocalTime endTime;

    @NotBlank(message = "Room is required")
    @Size(min = 1, max = 20, message = "Room must be between 1 and 20 characters")
    private String room;

    @NotNull(message = "trainerId is required")
    private UUID trainerId;

    @Min(value = 1, message = "Capacity must be at least 1")
    private int capacity;

    private boolean active = Boolean.TRUE;
}
