package rw.ac.auca.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.test.context.TestPropertySource;
import rw.ac.auca.attendance.domain.Attendance;
import rw.ac.auca.attendance.domain.AttendanceStatus;
import rw.ac.auca.course.domain.Course;
import rw.ac.auca.session.domain.Session;
import rw.ac.auca.student.domain.Student;
import rw.ac.auca.user.domain.Role;
import rw.ac.auca.user.domain.RoleName;
import rw.ac.auca.user.domain.User;

import java.time.LocalDate;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * The Class DatabaseConstraintsTest.
 *
 * The rules these cover are the ones the application must not be trusted with.
 * A service can be bypassed by a bug or by a second caller; a UNIQUE constraint
 * cannot. These tests assert the database itself refuses the writes, which is
 * what makes "one record per student per session" a guarantee rather than a
 * hope.
 *
 * These run against a real PostgreSQL database (aicattendance_test, recreated
 * on each run), not an embedded one. That is deliberate: a constraint is a
 * property of the database, so checking it on a different engine would be
 * testing the wrong thing. H2 proved the point - it rejects a valid value
 * against the generated enum check, which PostgreSQL accepts.
 *
 * It does mean these tests need the server the application already requires.
 * The connection details come from TEST_DB_URL, TEST_DB_USER and
 * TEST_DB_PASSWORD so the same test runs on a laptop and in CI.
 *
 * @author Ishimwe Mireille
 * @version 1.0
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource(properties = {
        "spring.datasource.url=${TEST_DB_URL:jdbc:postgresql://localhost:5432/aicattendance_test}",
        "spring.datasource.username=${TEST_DB_USER:postgres}",
        "spring.datasource.password=${TEST_DB_PASSWORD:1234}",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
class DatabaseConstraintsTest {

    @Autowired
    private TestEntityManager entityManager;

    private User trainer;
    private Course course;

    @BeforeEach
    void setUp() {
        Role role = entityManager.persist(new Role(RoleName.TRAINER));
        trainer = new User();
        trainer.setEmail("trainer.web@auca.ac.rw");
        trainer.setFullName("Jean Nsengiyumva");
        trainer.getRoles().add(role);
        trainer = entityManager.persist(trainer);

        course = new Course();
        course.setCode("WEBTECH");
        course.setTitle("Web Technology");
        course.setSequence(1);
        course.setStartDate(LocalDate.of(2026, 10, 1));
        course.setEndDate(LocalDate.of(2026, 10, 31));
        course.setTrainer(trainer);
        course = entityManager.persist(course);
    }

    private Student student(String regNumber, String email) {
        Student student = new Student();
        student.setRegNumber(regNumber);
        student.setFirstName("Aline");
        student.setLastName("Uwase");
        student.setEmail(email);
        student.setPhone("0788123456");
        student.setGender("Female");
        student.setSector("Health");
        student.setStudentGroup("GROUP A");
        student.setYearOfStudy(3);
        student.setActive(true);
        return student;
    }

    private Session session(String room, LocalTime start) {
        Session session = new Session();
        session.setTitle("Web Technology");
        session.setSessionDate(LocalDate.of(2026, 10, 10));
        session.setStartTime(start);
        session.setEndTime(start.plusHours(2));
        session.setRoom(room);
        session.setCourse(course);
        session.setCapacity(40);
        session.setActive(true);
        return session;
    }

    private Attendance attendance(Student student, Session session) {
        Attendance attendance = new Attendance();
        attendance.setStudent(student);
        attendance.setSession(session);
        attendance.setRecordedBy(trainer);
        attendance.setStatus(AttendanceStatus.PRESENT);
        attendance.setTimeIn(LocalTime.of(7, 56));
        return attendance;
    }

    // ----------------------------------------------- the central guarantee

    @Test
    @DisplayName("the database refuses a second attendance record for one student and session")
    void refusesDuplicateAttendance() {
        Student aline = entityManager.persist(student("24011", "aline.24011@auca.ac.rw"));
        Session web = entityManager.persist(session("Lab 204", LocalTime.of(8, 0)));
        entityManager.persist(attendance(aline, web));
        entityManager.flush();

        entityManager.persist(attendance(aline, web));

        assertThatThrownBy(() -> entityManager.flush())
                .isInstanceOf(ConstraintViolationException.class)
                .hasMessageContaining("uk_student_session");
    }

    @Test
    @DisplayName("but the same student may be marked in two different sessions")
    void allowsTheSameStudentInAnotherSession() {
        Student aline = entityManager.persist(student("24011", "aline.24011@auca.ac.rw"));
        Session morning = entityManager.persist(session("Lab 204", LocalTime.of(8, 0)));
        Session afternoon = entityManager.persist(session("Lab 201", LocalTime.of(14, 0)));

        entityManager.persist(attendance(aline, morning));
        entityManager.persist(attendance(aline, afternoon));

        assertThatCode(() -> entityManager.flush()).doesNotThrowAnyException();
    }

    // ----------------------------------------------------------- students

    @Test
    @DisplayName("the database refuses two students with the same registration number")
    void refusesDuplicateRegistrationNumber() {
        entityManager.persist(student("24011", "aline.24011@auca.ac.rw"));
        entityManager.flush();

        entityManager.persist(student("24011", "someone.else@auca.ac.rw"));

        assertThatThrownBy(() -> entityManager.flush())
                .isInstanceOf(ConstraintViolationException.class)
                .hasMessageContaining("reg_number");
    }

    @Test
    @DisplayName("the database refuses two students with the same email")
    void refusesDuplicateEmail() {
        entityManager.persist(student("24011", "aline.24011@auca.ac.rw"));
        entityManager.flush();

        entityManager.persist(student("24102", "aline.24011@auca.ac.rw"));

        assertThatThrownBy(() -> entityManager.flush())
                .isInstanceOf(ConstraintViolationException.class)
                .hasMessageContaining("email");
    }

    // ----------------------------------------------------------- sessions

    @Test
    @DisplayName("the database refuses two sessions in one room at the same date and time")
    void refusesRoomClash() {
        entityManager.persist(session("Lab 204", LocalTime.of(8, 0)));
        entityManager.flush();

        entityManager.persist(session("Lab 204", LocalTime.of(8, 0)));

        assertThatThrownBy(() -> entityManager.flush())
                .isInstanceOf(ConstraintViolationException.class)
                .hasMessageContaining("uk_session_date_time_room");
    }

    @Test
    @DisplayName("but the same room may be used later the same day")
    void allowsTheSameRoomAtAnotherTime() {
        entityManager.persist(session("Lab 204", LocalTime.of(8, 0)));
        entityManager.persist(session("Lab 204", LocalTime.of(14, 0)));

        assertThatCode(() -> entityManager.flush()).doesNotThrowAnyException();
    }

    // -------------------------------------------------------------- roles

    @Test
    @DisplayName("the database refuses two users with the same email")
    void refusesDuplicateUserEmail() {
        User duplicate = new User();
        duplicate.setEmail("trainer.web@auca.ac.rw");
        duplicate.setFullName("Someone Else");
        entityManager.persist(duplicate);

        assertThatThrownBy(() -> entityManager.flush())
                .isInstanceOf(ConstraintViolationException.class)
                .hasMessageContaining("email");
    }
}
