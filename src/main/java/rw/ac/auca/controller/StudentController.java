package rw.ac.auca.controller;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import rw.ac.auca.student.domain.Student;
import rw.ac.auca.student.service.StudentImportResult;
import rw.ac.auca.student.service.StudentImportService;
import rw.ac.auca.student.service.StudentService;
import org.springframework.web.multipart.MultipartFile;

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

    @Autowired
    private StudentImportService studentImportService;

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

    /**
     * Registering a cohort one form at a time is the slowest part of setting the
     * system up, so a spreadsheet can be uploaded instead. Each row is validated
     * exactly as the JSON endpoint would be.
     */
    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN', 'COHORT_LEADER')")
    public StudentImportResult importStudents(@RequestParam("file") MultipartFile file) {
        return studentImportService.importFrom(file);
    }

    /** An empty spreadsheet with the right columns, so nobody has to guess. */
    @GetMapping("/import/template")
    @PreAuthorize("hasAnyRole('ADMIN', 'COHORT_LEADER')")
    public ResponseEntity<byte[]> importTemplate() {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"student-import-template.xlsx\"")
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(studentImportService.template());
    }
}
