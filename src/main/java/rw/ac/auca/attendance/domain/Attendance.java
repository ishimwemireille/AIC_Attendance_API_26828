package rw.ac.auca.attendance.domain;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Check;
import rw.ac.auca.base.BaseEntity;
import rw.ac.auca.session.domain.Session;
import rw.ac.auca.student.domain.Student;

import java.time.LocalTime;

/**
 * The Class Attendance.
 *
 * @author Ishimwe Mireille
 * @version 1.0
 */
@Getter
@Setter
@Entity
@Table(name = "attendances",
        uniqueConstraints = @UniqueConstraint(name = "uk_student_session",
                columnNames = {"student_id", "session_id"}))
@Check(constraints = "status IN ('PRESENT', 'LATE', 'ABSENT', 'EXCUSED')")
public class Attendance extends BaseEntity {

    @NotNull(message = "Student is required")
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @NotNull(message = "Session is required")
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "session_id", nullable = false)
    private Session session;

    @JsonFormat(pattern = "HH:mm")
    @Column(name = "time_in")
    private LocalTime timeIn;

    @NotNull(message = "Status is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 10)
    private AttendanceStatus status;

    @Size(max = 150, message = "Remarks cannot be longer than 150 characters")
    @Column(name = "remarks", length = 150)
    private String remarks;
}
