package rw.ac.auca.user.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import rw.ac.auca.user.domain.Role;
import rw.ac.auca.user.domain.RoleName;

import java.util.Optional;
import java.util.UUID;

/**
 * The Interface RoleRepository.
 *
 * @author Ishimwe Mireille
 * @version 1.0
 */
@Repository
public interface RoleRepository extends JpaRepository<Role, UUID> {

    Optional<Role> findByName(RoleName name);
}
