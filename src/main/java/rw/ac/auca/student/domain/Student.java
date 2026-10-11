package rw.ac.auca.student.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Check;
import rw.ac.auca.base.BaseEntity;

/**
 * The Class Student.
 *
 * @author Ishimwe Mireille
 * @version 1.0
 */
@Getter
@Setter
@Entity
@Table(name = "students", indexes = {
        @Index(name = "idx_students_area_group", columnList = "thematic_area, student_group"),
        @Index(name = "idx_students_active", columnList = "active")
})
@Check(constraints = "LENGTH(first_name) >= 2 AND LENGTH(last_name) >= 2 "
        + "AND gender IN ('Male', 'Female')")
public class Student extends BaseEntity {

    @NotBlank(message = "Registration number is required")
    @Pattern(regexp = "[0-9]{5}", message = "Registration number must be exactly 5 digits")
    @Column(name = "reg_number", nullable = false, unique = true, length = 5)
    private String regNumber;

    @NotBlank(message = "First name is required")
    @Size(min = 2, max = 30, message = "First name must be between 2 and 30 characters")
    @Column(name = "first_name", nullable = false, length = 30)
    private String firstName;

    @NotBlank(message = "Last name is required")
    @Size(min = 2, max = 30, message = "Last name must be between 2 and 30 characters")
    @Column(name = "last_name", nullable = false, length = 30)
    private String lastName;

    /**
     * Optional. The attendance register the centre keeps carries no contact
     * details, so a student may have none. Still unique when present, and a
     * student without one simply receives no email alert.
     */
    @Email(message = "Email must be a valid address")
    @Column(name = "email", unique = true, length = 60)
    private String email;

    /** Optional, for the same reason as the email. */
    @Pattern(regexp = "07[0-9]{8}|", message = "Phone must be 10 digits starting with 07")
    @Column(name = "phone", length = 10)
    private String phone;

    @NotBlank(message = "Gender is required")
    @Pattern(regexp = "Male|Female", message = "Gender must be Male or Female")
    @Column(name = "gender", nullable = false, length = 6)
    private String gender;

    @NotBlank(message = "Thematic area is required")
    @Size(min = 2, max = 40, message = "Thematic area must be between 2 and 40 characters")
    /**
     * The thematic area the student works in: Agriculture, Crime Prevention,
     * Education, Fintech or Health. The register spells the column SECTOR.
     */
    @Column(name = "thematic_area", nullable = false, length = 40)
    private String thematicArea;

    /**
     * Which of the teaching groups the student sits in. They all take the same
     * course at the same time; the split is only for room size, so this is a
     * label rather than a relationship.
     */
    @Size(max = 20, message = "Group cannot be longer than 20 characters")
    @Column(name = "student_group", length = 20)
    private String studentGroup;


    @Column(name = "active", nullable = false)
    private boolean active = Boolean.TRUE;

    public String getFullName() {
        return firstName + " " + lastName;
    }
}
