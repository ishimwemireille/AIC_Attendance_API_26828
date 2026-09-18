package rw.ac.auca.session.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rw.ac.auca.attendance.repository.AttendanceRepository;
import rw.ac.auca.exception.BusinessRuleException;
import rw.ac.auca.exception.ResourceNotFoundException;
import rw.ac.auca.session.domain.Session;
import rw.ac.auca.session.repository.SessionRepository;

import java.util.List;
import java.util.UUID;

/**
 * The Class SessionServiceImpl.
 *
 * @author Ishimwe Mireille
 * @version 1.0
 */
@Service
@Transactional
public class SessionServiceImpl implements SessionService {

    @Autowired
    private SessionRepository sessionRepository;

    @Autowired
    private AttendanceRepository attendanceRepository;

    @Override
    public Session createSession(Session theSession) {
        checkTimes(theSession);
        if (sessionRepository.existsBySessionDateAndStartTimeAndRoomIgnoreCase(
                theSession.getSessionDate(), theSession.getStartTime(), theSession.getRoom())) {
            throw new BusinessRuleException("Room " + theSession.getRoom()
                    + " is already booked on " + theSession.getSessionDate()
                    + " at " + theSession.getStartTime() + ".");
        }
        return sessionRepository.save(theSession);
    }

    @Override
    public Session updateSession(UUID id, Session theSession) {
        Session found = findSessionById(id);
        checkTimes(theSession);

        if (sessionRepository.existsBySessionDateAndStartTimeAndRoomIgnoreCaseAndIdNot(
                theSession.getSessionDate(), theSession.getStartTime(), theSession.getRoom(), id)) {
            throw new BusinessRuleException("Room " + theSession.getRoom()
                    + " is already booked on " + theSession.getSessionDate()
                    + " at " + theSession.getStartTime() + ".");
        }

        long marked = attendanceRepository.countBySessionId(id);
        if (marked > theSession.getCapacity()) {
            throw new BusinessRuleException("Capacity cannot be reduced to "
                    + theSession.getCapacity() + " because " + marked
                    + " student(s) are already marked for this session.");
        }

        found.setTitle(theSession.getTitle());
        found.setSessionDate(theSession.getSessionDate());
        found.setStartTime(theSession.getStartTime());
        found.setEndTime(theSession.getEndTime());
        found.setRoom(theSession.getRoom());
        found.setTrainer(theSession.getTrainer());
        found.setCapacity(theSession.getCapacity());
        found.setActive(theSession.isActive());
        return sessionRepository.save(found);
    }

    private void checkTimes(Session theSession) {
        if (!theSession.getEndTime().isAfter(theSession.getStartTime())) {
            throw new BusinessRuleException("End time must be after the start time.");
        }
    }

    @Override
    public void deleteSession(UUID id) {
        Session found = findSessionById(id);
        long marked = attendanceRepository.countBySessionId(id);
        if (marked > 0) {
            throw new BusinessRuleException("Cannot delete this session because "
                    + marked + " attendance record(s) exist for it.");
        }
        sessionRepository.delete(found);
    }

    @Override
    @Transactional(readOnly = true)
    public Session findSessionById(UUID id) {
        return sessionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("No session found with id " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Session> findAllSessions() {
        return sessionRepository.findAllByOrderBySessionDateDescStartTimeAsc();
    }
}
