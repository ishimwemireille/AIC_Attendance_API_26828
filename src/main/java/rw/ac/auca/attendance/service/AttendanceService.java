package rw.ac.auca.attendance.service;

import rw.ac.auca.attendance.domain.Attendance;

import java.util.List;
import java.util.UUID;

/**
 * The Interface AttendanceService.
 *
 * @author Ishimwe Mireille
 * @version 1.0
 */
public interface AttendanceService {
    Attendance markAttendance(AttendanceRequest request);
    Attendance updateAttendance(UUID id, AttendanceRequest request);
    void deleteAttendance(UUID id);
    Attendance findAttendanceById(UUID id);
    List<Attendance> findAllAttendances();
    List<Attendance> findBySession(UUID sessionId);
    List<Attendance> findByStudent(UUID studentId);
}
