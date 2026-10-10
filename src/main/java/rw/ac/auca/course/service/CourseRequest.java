package rw.ac.auca.course.service;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.UUID;

/**
 * The Class CourseRequest.
 *
 * @author Ishimwe Mireille
 * @version 1.0
 */
@Getter
@Setter
public class CourseRequest {

    @NotBlank(message = "Course code is required")
    @Size(min = 2, max = 20, message = "Course code must be between 2 and 20 characters")
    private String code;

    @NotBlank(message = "Title is required")
    @Size(min = 3, max = 80, message = "Title must be between 3 and 80 characters")
    private String title;

    @Size(max = 300, message = "Description cannot be longer than 300 characters")
    private String description;

    @Min(value = 1, message = "Sequence must be at least 1")
    private int sequence;

    @NotNull(message = "Start date is required")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate startDate;

    @NotNull(message = "End date is required")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate endDate;

    @NotNull(message = "trainerId is required")
    private UUID trainerId;

    private boolean active = Boolean.TRUE;
}
