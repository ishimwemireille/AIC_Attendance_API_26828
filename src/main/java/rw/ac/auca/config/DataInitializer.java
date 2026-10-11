package rw.ac.auca.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import rw.ac.auca.user.domain.Role;
import rw.ac.auca.user.domain.RoleName;
import rw.ac.auca.user.domain.User;
import rw.ac.auca.user.repository.RoleRepository;
import rw.ac.auca.course.domain.Course;
import rw.ac.auca.course.repository.CourseRepository;
import rw.ac.auca.session.domain.Session;
import rw.ac.auca.session.repository.SessionRepository;
import rw.ac.auca.user.repository.UserRepository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Set;

/**
 * The Class DataInitializer. Creates the three roles and the first
 * administrator, so that a fresh database is usable immediately.
 *
 * @author Ishimwe Mireille
 * @version 1.0
 */
@Component
public class DataInitializer implements CommandLineRunner {

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private SessionRepository sessionRepository;

    @Value("${app.admin.email}")
    private String adminEmail;

    @Value("${app.admin.name}")
    private String adminName;

    @Value("${app.seed-demo-data:false}")
    private boolean seedDemoData;

    @Override
    @Transactional
    public void run(String... args) {
        for (RoleName name : RoleName.values()) {
            roleRepository.findByName(name).orElseGet(() -> roleRepository.save(new Role(name)));
        }

        ensureUser(adminEmail, adminName, Set.of(RoleName.ADMIN));

        if (seedDemoData) {
            ensureUser("cohort.leader@auca.ac.rw", "Mireille Ishimwe",
                    Set.of(RoleName.COHORT_LEADER));
            ensureUser("trainer.devops@auca.ac.rw", "Jean Nsengiyumva", Set.of(RoleName.TRAINER));
            ensureUser("trainer.web@auca.ac.rw", "Alice Kalisa", Set.of(RoleName.TRAINER));
            ensureUser("trainer.design@auca.ac.rw", "Claudine Mukeshimana", Set.of(RoleName.TRAINER));
            ensureUser("trainer.mobile@auca.ac.rw", "Eric Bizimana", Set.of(RoleName.TRAINER));
            ensureUser("trainer.ai@auca.ac.rw", "Patrick Rukundo", Set.of(RoleName.TRAINER));
            ensureUser("trainer.iot@auca.ac.rw", "Sandrine Uwera", Set.of(RoleName.TRAINER));
            seedProgramme();
        }
    }

    /**
     * The cohort takes these one after another, so the date ranges are
     * consecutive and never overlap. Only seeded into an empty database.
     */
    private void seedProgramme() {
        if (courseRepository.count() > 0) {
            return;
        }
        LocalDate start = LocalDate.now().minusWeeks(10).with(java.time.DayOfWeek.MONDAY);

        String[][] programme = {
                {"DEVOPS", "DevOps", "trainer.devops@auca.ac.rw",
                 "Version control, containers, pipelines and deployment."},
                {"WEBTECH", "Web Technology", "trainer.web@auca.ac.rw",
                 "Server side web development with Spring Boot and REST."},
                {"UIUX", "Internet UI and UX", "trainer.design@auca.ac.rw",
                 "Interface design, prototyping and usability for the web."},
                {"MOBILE", "Mobile Programming", "trainer.mobile@auca.ac.rw",
                 "Building and shipping applications for mobile devices."},
                {"AI", "Artificial Intelligence", "trainer.ai@auca.ac.rw",
                 "Machine learning foundations and applied models."},
                {"IOT", "Internet of Things", "trainer.iot@auca.ac.rw",
                 "Connected devices, sensors and the platforms behind them."},
        };

        for (int index = 0; index < programme.length; index++) {
            String[] row = programme[index];
            LocalDate from = start.plusWeeks(index * 4L);
            LocalDate to = from.plusWeeks(4L).minusDays(3);

            Course course = new Course();
            course.setCode(row[0]);
            course.setTitle(row[1]);
            course.setDescription(row[3]);
            course.setSequence(index + 1);
            course.setStartDate(from);
            course.setEndDate(to);
            userRepository.findByEmailIgnoreCase(row[2]).ifPresent(course::setTrainer);
            if (course.getTrainer() == null) {
                continue;
            }
            Course saved = courseRepository.save(course);
            seedSessions(saved);
        }
    }

    /** Two sittings a week, only for the dates that have already passed. */
    private void seedSessions(Course course) {
        LocalDate day = course.getStartDate();
        int count = 0;
        while (!day.isAfter(course.getEndDate()) && !day.isAfter(LocalDate.now()) && count < 8) {
            if (day.getDayOfWeek() == java.time.DayOfWeek.MONDAY
                    || day.getDayOfWeek() == java.time.DayOfWeek.WEDNESDAY) {
                Session session = new Session();
                session.setCourse(course);
                session.setTitle(course.getTitle() + " - session " + (count + 1));
                session.setSessionDate(day);
                session.setStartTime(LocalTime.of(8, 0));
                session.setEndTime(LocalTime.of(10, 0));
                session.setRoom("Lab 20" + (course.getSequence() % 5 + 1));
                session.setCapacity(40);
                sessionRepository.save(session);
                count++;
            }
            day = day.plusDays(1);
        }
    }

    private void ensureUser(String email, String fullName, Set<RoleName> roleNames) {
        if (userRepository.existsByEmailIgnoreCase(email)) {
            return;
        }
        User user = new User();
        user.setEmail(email.toLowerCase());
        user.setFullName(fullName);
        roleNames.forEach(name -> roleRepository.findByName(name).ifPresent(user.getRoles()::add));
        userRepository.save(user);
    }
}
