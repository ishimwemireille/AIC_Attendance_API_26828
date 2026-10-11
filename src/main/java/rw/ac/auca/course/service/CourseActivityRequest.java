package rw.ac.auca.course.service;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

/**
 * The Class CourseActivityRequest.
 *
 * The course and the author are not sent by the client: the course comes from
 * the path and the author from the signed-in user, so neither can be claimed.
 *
 * @author Ishimwe Mireille
 * @version 1.0
 */
@Getter
@Setter
public class CourseActivityRequest {

    @NotNull(message = "The date is required")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate activityDate;

    @NotBlank(message = "A title is required")
    @Size(min = 3, max = 100, message = "The title must be between 3 and 100 characters")
    private String title;

    @Size(max = 600, message = "Notes cannot be longer than 600 characters")
    private String notes;
}
