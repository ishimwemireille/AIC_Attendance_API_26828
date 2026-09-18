package rw.ac.auca.attendance.service;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import rw.ac.auca.attendance.domain.AttendanceStatus;

import java.time.LocalTime;
import java.util.UUID;

/**
 * The Class AttendanceRequest. What Postman sends: ids instead of whole objects.
 *
 * @author Ishimwe Mireille
 * @version 1.0
 */
@Getter
@Setter
public class AttendanceRequest {

    @NotNull(message = "studentId is required")
    private UUID studentId;

    @NotNull(message = "sessionId is required")
    private UUID sessionId;

    @JsonFormat(pattern = "HH:mm")
    private LocalTime timeIn;

    @NotNull(message = "status is required (PRESENT, LATE, ABSENT or EXCUSED)")
    private AttendanceStatus status;

    @Size(max = 150, message = "Remarks cannot be longer than 150 characters")
    private String remarks;
}
