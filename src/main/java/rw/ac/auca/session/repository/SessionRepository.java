package rw.ac.auca.session.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import rw.ac.auca.session.domain.Session;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface SessionRepository extends JpaRepository<Session, UUID> {

    List<Session> findAllByOrderBySessionDateDescStartTimeAsc();

    boolean existsBySessionDateAndStartTimeAndRoomIgnoreCase(
            LocalDate sessionDate, LocalTime startTime, String room);

    boolean existsBySessionDateAndStartTimeAndRoomIgnoreCaseAndIdNot(
            LocalDate sessionDate, LocalTime startTime, String room, UUID id);
}
