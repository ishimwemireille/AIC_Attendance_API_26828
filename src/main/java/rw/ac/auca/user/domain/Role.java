package rw.ac.auca.user.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Check;
import rw.ac.auca.base.BaseEntity;

/**
 * The Class Role. A named set of permissions.
 *
 * @author Ishimwe Mireille
 * @version 1.0
 */
@Getter
@Setter
@Entity
@Table(name = "roles")
@Check(constraints = "name IN ('COHORT_LEADER', 'TRAINER', 'ADMIN')")
public class Role extends BaseEntity {

    @NotNull(message = "Role name is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "name", nullable = false, unique = true, length = 20)
    private RoleName name;

    public Role() {
    }

    public Role(RoleName name) {
        this.name = name;
    }
}
