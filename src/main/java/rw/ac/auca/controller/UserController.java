package rw.ac.auca.controller;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import rw.ac.auca.course.service.CourseService;
import rw.ac.auca.user.domain.RoleName;
import rw.ac.auca.user.domain.User;
import rw.ac.auca.user.service.CurrentUserService;
import rw.ac.auca.user.service.UserRequest;
import rw.ac.auca.user.service.UserService;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * The Class UserController. Managing who may use the system is an ADMIN job only.
 *
 * @author Ishimwe Mireille
 * @version 1.0
 */
@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private CourseService courseService;

    @Autowired
    private CurrentUserService currentUserService;

    /** Anyone signed in may ask who they are and what they are allowed to do. */
    @GetMapping("/me")
    public Map<String, Object> whoAmI() {
        User user = currentUserService.require();
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("id", user.getId());
        body.put("email", user.getEmail());
        body.put("fullName", user.getFullName());
        body.put("roles", user.getRoles().stream()
                .map(role -> role.getName().name())
                .sorted()
                .collect(Collectors.toList()));
        return body;
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public List<User> findAllUsers() {
        return userService.findAllUsers();
    }

    /** Needed to populate the trainer field when scheduling a session. */
    @GetMapping("/trainers")
    @PreAuthorize("hasAnyRole('ADMIN', 'COHORT_LEADER')")
    public List<User> findTrainers() {
        return userService.findTrainers();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public User findUserById(@PathVariable UUID id) {
        return userService.findUserById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public User createUser(@Valid @RequestBody UserRequest request) {
        User created = userService.createUser(
                request.getEmail(), request.getFullName(), request.getRoles());

        // A trainer with no course has nothing to open, so the course can be
        // named here. The course service still checks the role and the account.
        if (request.getCourseId() != null) {
            courseService.assignTrainer(request.getCourseId(), created.getId());
        }
        return created;
    }

    @PutMapping("/{id}/roles")
    @PreAuthorize("hasRole('ADMIN')")
    public User updateRoles(@PathVariable UUID id, @RequestBody Set<RoleName> roles) {
        return userService.updateRoles(id, roles);
    }

    @PutMapping("/{id}/active")
    @PreAuthorize("hasRole('ADMIN')")
    public User setActive(@PathVariable UUID id, @RequestParam boolean value) {
        return userService.setActive(id, value);
    }
}
