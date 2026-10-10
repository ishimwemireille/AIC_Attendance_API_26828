package rw.ac.auca.course.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import rw.ac.auca.base.BaseEntity;
import rw.ac.auca.user.domain.User;

import java.time.LocalDate;

/**
 * The Class CourseActivity. What a trainer did on their course and when.
 *
 * Attendance says who was in the room; this says what happened in it. Together
 * they are what a trainer needs to show progress through a course.
 *
 * @author Ishimwe Mireille
 * @version 1.0
 */
@Getter
@Setter
@Entity
@Table(name = "course_activities")
public class CourseActivity extends BaseEntity {

    @NotNull(message = "Course is required")
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @NotNull(message = "The date is required")
    @Column(name = "activity_date", nullable = false)
    private LocalDate activityDate;

    @NotBlank(message = "A title is required")
    @Size(min = 3, max = 100, message = "The title must be between 3 and 100 characters")
    @Column(name = "title", nullable = false, length = 100)
    private String title;

    @Size(max = 600, message = "Notes cannot be longer than 600 characters")
    @Column(name = "notes", length = 600)
    private String notes;

    @NotNull(message = "The author is required")
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;
}
