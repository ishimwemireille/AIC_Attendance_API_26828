package rw.ac.auca.session.service;

import rw.ac.auca.session.domain.Session;

import java.util.List;
import java.util.UUID;

/**
 * The Interface SessionService.
 *
 * @author Ishimwe Mireille
 * @version 1.0
 */
public interface SessionService {
    Session createSession(Session theSession);
    Session updateSession(UUID id, Session theSession);
    void deleteSession(UUID id);
    Session findSessionById(UUID id);
    List<Session> findAllSessions();
}
