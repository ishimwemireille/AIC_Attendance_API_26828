package rw.ac.auca.controller;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import rw.ac.auca.student.domain.Student;
import rw.ac.auca.student.service.StudentService;

import java.util.List;
import java.util.UUID;

/**
 * The Class StudentController.
 *
 * @author Ishimwe Mireille
 * @version 1.0
 */
@RestController
@RequestMapping("/api/students")
public class StudentController {

    @Autowired
    private StudentService studentService;

    @PreAuthorize("isAuthenticated()")
    @GetMapping
    public List<Student> findAllStudents() {
        return studentService.findAllStudents();
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/{id}")
    public Student findStudentById(@PathVariable UUID id) {
        return studentService.findStudentById(id);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'COHORT_LEADER')")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Student registerStudent(@Valid @RequestBody Student theStudent) {
        return studentService.registerStudent(theStudent);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'COHORT_LEADER')")
    @PutMapping("/{id}")
    public Student updateStudent(@PathVariable UUID id, @Valid @RequestBody Student theStudent) {
        return studentService.updateStudent(id, theStudent);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'COHORT_LEADER')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteStudent(@PathVariable UUID id) {
        studentService.deleteStudent(id);
        return ResponseEntity.noContent().build();
    }
}
