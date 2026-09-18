package rw.ac.auca.controller;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import rw.ac.auca.attendance.domain.Attendance;
import rw.ac.auca.attendance.service.AttendanceRequest;
import rw.ac.auca.attendance.service.AttendanceService;

import java.util.List;
import java.util.UUID;

/**
 * The Class AttendanceController.
 *
 * @author Ishimwe Mireille
 * @version 1.0
 */
@RestController
@RequestMapping("/api/attendances")
public class AttendanceController {

    @Autowired
    private AttendanceService attendanceService;

    @GetMapping
    public List<Attendance> findAllAttendances() {
        return attendanceService.findAllAttendances();
    }

    @GetMapping("/{id}")
    public Attendance findAttendanceById(@PathVariable UUID id) {
        return attendanceService.findAttendanceById(id);
    }

    @GetMapping("/session/{sessionId}")
    public List<Attendance> findBySession(@PathVariable UUID sessionId) {
        return attendanceService.findBySession(sessionId);
    }

    @GetMapping("/student/{studentId}")
    public List<Attendance> findByStudent(@PathVariable UUID studentId) {
        return attendanceService.findByStudent(studentId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Attendance markAttendance(@Valid @RequestBody AttendanceRequest request) {
        return attendanceService.markAttendance(request);
    }

    @PutMapping("/{id}")
    public Attendance updateAttendance(@PathVariable UUID id,
                                       @Valid @RequestBody AttendanceRequest request) {
        return attendanceService.updateAttendance(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAttendance(@PathVariable UUID id) {
        attendanceService.deleteAttendance(id);
        return ResponseEntity.noContent().build();
    }
}
