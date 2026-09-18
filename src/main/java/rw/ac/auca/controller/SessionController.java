package rw.ac.auca.controller;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import rw.ac.auca.session.domain.Session;
import rw.ac.auca.session.service.SessionService;

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

    @GetMapping
    public List<Session> findAllSessions() {
        return sessionService.findAllSessions();
    }

    @GetMapping("/{id}")
    public Session findSessionById(@PathVariable UUID id) {
        return sessionService.findSessionById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Session createSession(@Valid @RequestBody Session theSession) {
        return sessionService.createSession(theSession);
    }

    @PutMapping("/{id}")
    public Session updateSession(@PathVariable UUID id, @Valid @RequestBody Session theSession) {
        return sessionService.updateSession(id, theSession);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSession(@PathVariable UUID id) {
        sessionService.deleteSession(id);
        return ResponseEntity.noContent().build();
    }
}
