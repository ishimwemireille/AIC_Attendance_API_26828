package rw.ac.auca.controller;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import rw.ac.auca.session.domain.Session;
import rw.ac.auca.session.service.SessionRequest;
import rw.ac.auca.session.service.SessionService;
import rw.ac.auca.user.service.CurrentUserService;

import java.util.List;
import java.util.UUID;

/**
 * The Class SessionController.
 *
 * @author Ishimwe Mireille
 * @version 1.0
 */
@RestController
@RequestMapping("/api/sessions")
public class SessionController {

    @Autowired
    private SessionService sessionService;

    @Autowired
    private CurrentUserService currentUserService;

    /** A trainer gets only their own sessions back; see SessionServiceImpl. */
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public List<Session> findAllSessions() {
        return sessionService.findAllSessions();
    }

    @GetMapping("/mine")
    @PreAuthorize("isAuthenticated()")
    public List<Session> findMySessions() {
        return sessionService.findByTrainer(currentUserService.require().getId());
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public Session findSessionById(@PathVariable UUID id) {
        return sessionService.findSessionById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN', 'COHORT_LEADER')")
    public Session createSession(@Valid @RequestBody SessionRequest request) {
        return sessionService.createSession(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'COHORT_LEADER')")
    public Session updateSession(@PathVariable UUID id, @Valid @RequestBody SessionRequest request) {
        return sessionService.updateSession(id, request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'COHORT_LEADER')")
    public ResponseEntity<Void> deleteSession(@PathVariable UUID id) {
        sessionService.deleteSession(id);
        return ResponseEntity.noContent().build();
    }
}
