package rw.ac.auca.user.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Check;
import rw.ac.auca.base.BaseEntity;

import java.util.HashSet;
import java.util.Set;

/**
 * The Class User. A member of staff who may sign in.
 *
 * @author Ishimwe Mireille
 * @version 1.0
 */
@Getter
@Setter
@Entity
@Table(name = "users")
@Check(constraints = "LENGTH(full_name) >= 3")
public class User extends BaseEntity {

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be a valid address")
    @Column(name = "email", nullable = false, unique = true, length = 60)
    private String email;

    @NotBlank(message = "Full name is required")
    @Size(min = 3, max = 80, message = "Full name must be between 3 and 80 characters")
    @Column(name = "full_name", nullable = false, length = 80)
    private String fullName;

    /** The Google "sub" claim, stored on the first OAuth2 sign-in. No password is ever kept. */
    @JsonIgnore
    @Column(name = "google_subject", unique = true, length = 64)
    private String googleSubject;

    @Column(name = "active", nullable = false)
    private boolean active = Boolean.TRUE;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "user_roles",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id"))
    private Set<Role> roles = new HashSet<>();

    public boolean hasRole(RoleName roleName) {
        return roles.stream().anyMatch(role -> role.getName() == roleName);
    }
}
