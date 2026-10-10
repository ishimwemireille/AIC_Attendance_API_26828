package rw.ac.auca.course.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Check;
import rw.ac.auca.base.BaseEntity;
import rw.ac.auca.user.domain.User;

import java.time.LocalDate;

/**
 * The Class Course. One module of the programme, for example DevOps or Web
 * Technology.
 *
 * The cohort takes the courses one after another, so a course carries the
 * position it occupies in the programme and the dates it runs. Two courses may
 * not overlap, which is checked in the service because it spans rows.
 *
 * @author Ishimwe Mireille
 * @version 1.0
 */
@Getter
@Setter
@Entity
@Table(name = "courses")
@Check(constraints = "end_date > start_date AND sequence_number >= 1")
public class Course extends BaseEntity {

    @NotBlank(message = "Course code is required")
    @Size(min = 2, max = 20, message = "Course code must be between 2 and 20 characters")
    @Column(name = "code", nullable = false, unique = true, length = 20)
    private String code;

    @NotBlank(message = "Title is required")
    @Size(min = 3, max = 80, message = "Title must be between 3 and 80 characters")
    @Column(name = "title", nullable = false, unique = true, length = 80)
    private String title;

    @Size(max = 300, message = "Description cannot be longer than 300 characters")
    @Column(name = "description", length = 300)
    private String description;

    /** Where this course sits in the programme. 1 is taught first. */
    @Min(value = 1, message = "Sequence must be at least 1")
    @Column(name = "sequence_number", nullable = false, unique = true)
    private int sequence;

    @NotNull(message = "Start date is required")
    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @NotNull(message = "End date is required")
    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @NotNull(message = "Trainer is required")
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "trainer_id", nullable = false)
    private User trainer;

    @Column(name = "active", nullable = false)
    private boolean active = Boolean.TRUE;

    /** True when today falls inside the course's dates. */
    public boolean isRunning() {
        LocalDate today = LocalDate.now();
        return active && !today.isBefore(startDate) && !today.isAfter(endDate);
    }
}
