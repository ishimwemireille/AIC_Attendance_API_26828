package rw.ac.auca.user.service;

import rw.ac.auca.user.domain.RoleName;
import rw.ac.auca.user.domain.User;

import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * The Interface UserService.
 *
 * @author Ishimwe Mireille
 * @version 1.0
 */
public interface UserService {

    User createUser(String email, String fullName, Set<RoleName> roles);

    User updateRoles(UUID id, Set<RoleName> roles);

    User setActive(UUID id, boolean active);

    User findUserById(UUID id);

    User findUserByEmail(String email);

    List<User> findAllUsers();

    List<User> findTrainers();
}
