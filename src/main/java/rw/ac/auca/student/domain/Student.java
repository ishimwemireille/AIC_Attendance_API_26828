package rw.ac.auca.student.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
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
@Table(name = "students")
@Check(constraints = "LENGTH(first_name) >= 2 AND LENGTH(last_name) >= 2 "
        + "AND gender IN ('Male', 'Female') "
        + "AND year_of_study BETWEEN 1 AND 5")
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

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be a valid address")
    @Column(name = "email", nullable = false, unique = true, length = 60)
    private String email;

    @NotBlank(message = "Phone is required")
    @Pattern(regexp = "07[0-9]{8}", message = "Phone must be 10 digits starting with 07")
    @Column(name = "phone", nullable = false, length = 10)
    private String phone;

    @NotBlank(message = "Gender is required")
    @Pattern(regexp = "Male|Female", message = "Gender must be Male or Female")
    @Column(name = "gender", nullable = false, length = 6)
    private String gender;

    @NotBlank(message = "Program is required")
    @Size(min = 2, max = 40, message = "Program must be between 2 and 40 characters")
    @Column(name = "program", nullable = false, length = 40)
    private String program;

    @Min(value = 1, message = "Year of study must be at least 1")
    @Max(value = 5, message = "Year of study cannot be more than 5")
    @Column(name = "year_of_study", nullable = false)
    private int yearOfStudy;

    @Column(name = "active", nullable = false)
    private boolean active = Boolean.TRUE;

    public String getFullName() {
        return firstName + " " + lastName;
    }
}
