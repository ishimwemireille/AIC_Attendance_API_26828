package rw.ac.auca.user.service;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import rw.ac.auca.user.domain.RoleName;

import java.util.Set;

/**
 * The Class UserRequest. What an administrator sends to grant someone access.
 *
 * @author Ishimwe Mireille
 * @version 1.0
 */
@Getter
@Setter
public class UserRequest {

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be a valid address")
    private String email;

    @NotBlank(message = "Full name is required")
    @Size(min = 3, max = 80, message = "Full name must be between 3 and 80 characters")
    private String fullName;

    @NotEmpty(message = "At least one role is required")
    private Set<RoleName> roles;
}
