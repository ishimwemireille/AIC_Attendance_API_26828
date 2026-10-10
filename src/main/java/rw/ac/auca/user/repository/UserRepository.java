package rw.ac.auca.user.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import rw.ac.auca.user.domain.User;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * The Interface UserRepository.
 *
 * @author Ishimwe Mireille
 * @version 1.0
 */
@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    List<User> findAllByOrderByFullNameAsc();
}
