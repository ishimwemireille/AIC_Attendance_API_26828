package rw.ac.auca.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import rw.ac.auca.attendance.service.AttendanceService;
import rw.ac.auca.audit.repository.AuditEntryRepository;
import rw.ac.auca.course.service.CourseProgressService;
import rw.ac.auca.course.service.CourseService;
import rw.ac.auca.audit.service.AuditService;
import rw.ac.auca.config.SecurityConfig;
import rw.ac.auca.notification.repository.NotificationLogRepository;
import rw.ac.auca.session.service.SessionService;
import rw.ac.auca.student.service.StudentImportService;
import rw.ac.auca.student.service.StudentService;
import rw.ac.auca.user.service.AucaOidcUserService;
import rw.ac.auca.user.service.CurrentUserService;
import rw.ac.auca.user.service.UserService;

import java.util.UUID;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The Class AuthorizationMatrixTest.
 *
 * Section 6 of the Part 2 report states who may do what. Until this existed,
 * that matrix was guaranteed only by having been exercised by hand once. These
 * tests pin it: every protected endpoint is called under every role.
 *
 * No database, broker or Mongo is needed - this is a web slice, so the rules
 * are checked in isolation from everything they protect.
 *
 * @author Ishimwe Mireille
 * @version 1.0
 */
@WebMvcTest
@Import(SecurityConfig.class)
@ActiveProfiles("test")
class AuthorizationMatrixTest {

    /** Only this slice needs the controllers, so the scan lives here. */
    @TestConfiguration
    @ComponentScan(basePackages = {"rw.ac.auca.controller", "rw.ac.auca.exception"})
    static class WebComponents {
    }

    private static final String STUDENT_JSON = """
            {"regNumber":"24011","firstName":"Aline","lastName":"Uwase",
             "email":"aline.24011@auca.ac.rw","phone":"0788123456","gender":"Female",
             "program":"Information Technology","yearOfStudy":3}""";

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    /**
     * MockMvc is built by hand so that springSecurity() is definitely applied.
     * Without it every request arrives anonymous, and because this chain has no
     * authentication entry point outside the dev profile, each one is refused
     * with 403 - which would make the "may not" tests pass for the wrong reason.
     */
    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    @MockitoBean private StudentService studentService;
    @MockitoBean private StudentImportService studentImportService;
    @MockitoBean private SessionService sessionService;
    @MockitoBean private CourseService courseService;
    @MockitoBean private CourseProgressService courseProgressService;
    @MockitoBean private AttendanceService attendanceService;
    @MockitoBean private UserService userService;
    @MockitoBean private CurrentUserService currentUserService;
    @MockitoBean private AuditService auditService;
    @MockitoBean private AuditEntryRepository auditEntryRepository;
    @MockitoBean private NotificationLogRepository notificationLogRepository;
    @MockitoBean private AucaOidcUserService aucaOidcUserService;

    // ------------------------------------------------------- no credentials

    @Test
    @WithAnonymousUser
    @DisplayName("an anonymous caller is refused everywhere")
    void anonymousIsRefused() throws Exception {
        mockMvc.perform(get("/api/students")).andExpect(status().is4xxClientError());
        mockMvc.perform(get("/api/sessions")).andExpect(status().is4xxClientError());
        mockMvc.perform(get("/api/attendances")).andExpect(status().is4xxClientError());
        mockMvc.perform(get("/api/users")).andExpect(status().is4xxClientError());
    }

    // ------------------------------------------------- managing user access

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("an administrator may list users")
    void adminMayListUsers() throws Exception {
        mockMvc.perform(get("/api/users")).andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "COHORT_LEADER")
    @DisplayName("a cohort leader may not list users")
    void cohortLeaderMayNotListUsers() throws Exception {
        mockMvc.perform(get("/api/users")).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "TRAINER")
    @DisplayName("a trainer may not list users")
    void trainerMayNotListUsers() throws Exception {
        mockMvc.perform(get("/api/users")).andExpect(status().isForbidden());
    }

    // --------------------------------------------------------- writing data

    @Test
    @WithMockUser(roles = "TRAINER")
    @DisplayName("a trainer may not register a student")
    void trainerMayNotRegisterAStudent() throws Exception {
        mockMvc.perform(post("/api/students")
                        .contentType(MediaType.APPLICATION_JSON).content(STUDENT_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "COHORT_LEADER")
    @DisplayName("a cohort leader may register a student")
    void cohortLeaderMayRegisterAStudent() throws Exception {
        mockMvc.perform(post("/api/students")
                        .contentType(MediaType.APPLICATION_JSON).content(STUDENT_JSON))
                .andExpect(status().isCreated());
    }

    @Test
    @WithMockUser(roles = "TRAINER")
    @DisplayName("a trainer may not import a spreadsheet of students")
    void trainerMayNotImportStudents() throws Exception {
        mockMvc.perform(multipart("/api/students/import")
                        .file(new MockMultipartFile("file", "students.xlsx",
                                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                                new byte[]{1, 2, 3})))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "TRAINER")
    @DisplayName("a trainer may not mark attendance")
    void trainerMayNotMarkAttendance() throws Exception {
        String body = """
                {"studentId":"%s","sessionId":"%s","status":"PRESENT","timeIn":"07:56"}"""
                .formatted(UUID.randomUUID(), UUID.randomUUID());
        mockMvc.perform(post("/api/attendances")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "TRAINER")
    @DisplayName("a trainer may not schedule a session")
    void trainerMayNotScheduleASession() throws Exception {
        String body = """
                {"title":"Week 3","sessionDate":"2026-10-10","startTime":"08:00",
                 "endTime":"10:00","room":"Lab 204","courseId":"%s","capacity":40}"""
                .formatted(UUID.randomUUID());
        mockMvc.perform(post("/api/sessions")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isForbidden());
    }

    // ---------------------------------------------------------- reading data

    @Test
    @WithMockUser(roles = "TRAINER")
    @DisplayName("a trainer may read sessions; the narrowing to their own happens in the service")
    void trainerMayReadSessions() throws Exception {
        mockMvc.perform(get("/api/sessions")).andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "TRAINER")
    @DisplayName("a trainer may read courses; the narrowing to their own happens in the service")
    void trainerMayReadCourses() throws Exception {
        mockMvc.perform(get("/api/courses")).andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "TRAINER")
    @DisplayName("a trainer may not create a course")
    void trainerMayNotCreateACourse() throws Exception {
        String body = """
                {"code":"WEBTECH","title":"Web Technology","sequence":1,
                 "startDate":"2026-10-01","endDate":"2026-10-31","trainerId":"%s"}"""
                .formatted(UUID.randomUUID());
        mockMvc.perform(post("/api/courses")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "COHORT_LEADER")
    @DisplayName("a cohort leader may create a course")
    void cohortLeaderMayCreateACourse() throws Exception {
        String body = """
                {"code":"WEBTECH","title":"Web Technology","sequence":1,
                 "startDate":"2026-10-01","endDate":"2026-10-31","trainerId":"%s"}"""
                .formatted(UUID.randomUUID());
        mockMvc.perform(post("/api/courses")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("an administrator may read the audit trail")
    void adminMayReadTheAuditTrail() throws Exception {
        mockMvc.perform(get("/api/audit")).andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "COHORT_LEADER")
    @DisplayName("a cohort leader may not read the audit trail")
    void cohortLeaderMayNotReadTheAuditTrail() throws Exception {
        mockMvc.perform(get("/api/audit")).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "TRAINER")
    @DisplayName("a trainer may not read the notification log")
    void trainerMayNotReadNotifications() throws Exception {
        mockMvc.perform(get("/api/notifications")).andExpect(status().isForbidden());
    }
}
