package rw.ac.auca.user.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rw.ac.auca.exception.BusinessRuleException;
import rw.ac.auca.exception.ResourceNotFoundException;
import rw.ac.auca.user.domain.Role;
import rw.ac.auca.user.domain.RoleName;
import rw.ac.auca.user.domain.User;
import rw.ac.auca.user.repository.RoleRepository;
import rw.ac.auca.user.repository.UserRepository;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * The Class UserServiceImpl.
 *
 * @author Ishimwe Mireille
 * @version 1.0
 */
@Service
@Transactional
public class UserServiceImpl implements UserService {

    /** Only an AUCA account may be given access to the system. */
    public static final String ALLOWED_DOMAIN = "@auca.ac.rw";

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Override
    public User createUser(String email, String fullName, Set<RoleName> roleNames) {
        String address = email == null ? "" : email.trim().toLowerCase();

        if (!address.endsWith(ALLOWED_DOMAIN)) {
            throw new BusinessRuleException("Only an " + ALLOWED_DOMAIN
                    + " address can be given access to the system.");
        }
        if (userRepository.existsByEmailIgnoreCase(address)) {
            throw new BusinessRuleException("A user with the email " + address + " already exists.");
        }
        if (roleNames == null || roleNames.isEmpty()) {
            throw new BusinessRuleException("A user must be given at least one role.");
        }

        User user = new User();
        user.setEmail(address);
        user.setFullName(fullName);
        user.setRoles(resolve(roleNames));
        return userRepository.save(user);
    }

    @Override
    public User updateRoles(UUID id, Set<RoleName> roleNames) {
        if (roleNames == null || roleNames.isEmpty()) {
            throw new BusinessRuleException("A user must keep at least one role.");
        }
        User found = findUserById(id);
        found.setRoles(resolve(roleNames));
        return userRepository.save(found);
    }

    @Override
    public User setActive(UUID id, boolean active) {
        User found = findUserById(id);
        if (!active && found.hasRole(RoleName.ADMIN) && countActiveAdmins() <= 1) {
            throw new BusinessRuleException(
                    "This is the last active administrator and cannot be deactivated.");
        }
        found.setActive(active);
        return userRepository.save(found);
    }

    private long countActiveAdmins() {
        return userRepository.findAll().stream()
                .filter(User::isActive)
                .filter(user -> user.hasRole(RoleName.ADMIN))
                .count();
    }

    private Set<Role> resolve(Set<RoleName> roleNames) {
        return roleNames.stream()
                .map(name -> roleRepository.findByName(name)
                        .orElseThrow(() -> new ResourceNotFoundException(
                                "The role " + name + " does not exist.")))
                .collect(Collectors.toCollection(HashSet::new));
    }

    @Override
    @Transactional(readOnly = true)
    public User findUserById(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("No user found with id " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public User findUserByEmail(String email) {
        return userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResourceNotFoundException("No user found for " + email));
    }

    @Override
    @Transactional(readOnly = true)
    public List<User> findAllUsers() {
        return userRepository.findAllByOrderByFullNameAsc();
    }
}
