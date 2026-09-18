package rw.ac.auca.attendance.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import rw.ac.auca.attendance.domain.Attendance;

import java.util.List;
import java.util.UUID;

@Repository
public interface AttendanceRepository extends JpaRepository<Attendance, UUID> {

    @Query("SELECT a FROM Attendance a JOIN FETCH a.student JOIN FETCH a.session "
            + "ORDER BY a.session.sessionDate DESC, a.session.startTime ASC")
    List<Attendance> findAllWithDetails();

    @Query("SELECT a FROM Attendance a JOIN FETCH a.student JOIN FETCH a.session "
            + "WHERE a.session.id = :sessionId")
    List<Attendance> findBySessionId(@Param("sessionId") UUID sessionId);

    @Query("SELECT a FROM Attendance a JOIN FETCH a.student JOIN FETCH a.session "
            + "WHERE a.student.id = :studentId")
    List<Attendance> findByStudentId(@Param("studentId") UUID studentId);

    boolean existsByStudentIdAndSessionId(UUID studentId, UUID sessionId);

    boolean existsByStudentIdAndSessionIdAndIdNot(UUID studentId, UUID sessionId, UUID id);

    long countBySessionId(UUID sessionId);

    long countByStudentId(UUID studentId);
}
