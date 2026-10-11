package rw.ac.auca.controller;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import rw.ac.auca.course.domain.Course;
import rw.ac.auca.course.service.CourseRequest;
import rw.ac.auca.course.domain.CourseActivity;
import rw.ac.auca.course.service.CourseActivityRequest;
import rw.ac.auca.course.service.CourseProgress;
import rw.ac.auca.course.service.CourseProgressService;
import rw.ac.auca.course.service.CourseService;
import rw.ac.auca.register.RegisterImportResult;
import rw.ac.auca.register.RegisterImportService;
import rw.ac.auca.user.service.CurrentUserService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

/**
 * The Class CourseController.
 *
 * @author Ishimwe Mireille
 * @version 1.0
 */
@RestController
@RequestMapping("/api/courses")
public class CourseController {

    @Autowired
    private CourseService courseService;

    @Autowired
    private CourseProgressService courseProgressService;

    @Autowired
    private RegisterImportService registerImportService;

    @Autowired
    private CurrentUserService currentUserService;

    /** A trainer gets only the courses they lead; see CourseServiceImpl. */
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public List<Course> findAllCourses() {
        return courseService.findAllCourses();
    }

    @GetMapping("/mine")
    @PreAuthorize("isAuthenticated()")
    public List<Course> findMyCourses() {
        return courseService.findByTrainer(currentUserService.require().getId());
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public Course findCourseById(@PathVariable UUID id) {
        return courseService.findCourseById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN', 'COHORT_LEADER')")
    public Course createCourse(@Valid @RequestBody CourseRequest request) {
        return courseService.createCourse(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'COHORT_LEADER')")
    public Course updateCourse(@PathVariable UUID id, @Valid @RequestBody CourseRequest request) {
        return courseService.updateCourse(id, request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'COHORT_LEADER')")
    public ResponseEntity<Void> deleteCourse(@PathVariable UUID id) {
        courseService.deleteCourse(id);
        return ResponseEntity.noContent().build();
    }

    /** The trainer's dashboard for one course. Scoped by findCourseById. */
    @GetMapping("/{id}/progress")
    @PreAuthorize("isAuthenticated()")
    public CourseProgress progress(@PathVariable UUID id) {
        return courseProgressService.progressOf(id);
    }

    @GetMapping("/{id}/activities")
    @PreAuthorize("isAuthenticated()")
    public List<CourseActivity> activities(@PathVariable UUID id) {
        return courseProgressService.activitiesOf(id);
    }

    /** A trainer logs what they covered; only on a course they lead. */
    @PostMapping("/{id}/activities")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("isAuthenticated()")
    public CourseActivity logActivity(@PathVariable UUID id,
                                      @Valid @RequestBody CourseActivityRequest request) {
        return courseProgressService.logActivity(id, request);
    }

    /**
     * Upload the attendance register the centre already keeps. One file brings
     * in the roster, a session for every date column, and the attendance.
     */
    @PostMapping(value = "/{id}/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN', 'COHORT_LEADER')")
    public RegisterImportResult importRegister(@PathVariable UUID id,
                                               @RequestParam("file") MultipartFile file) {
        return registerImportService.importInto(id, file);
    }

    /** An empty register in the centre's own shape. */
    @GetMapping("/import/template")
    @PreAuthorize("hasAnyRole('ADMIN', 'COHORT_LEADER')")
    public ResponseEntity<byte[]> importTemplate() {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"attendance-register-template.xlsx\"")
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(registerImportService.template());
    }
}
