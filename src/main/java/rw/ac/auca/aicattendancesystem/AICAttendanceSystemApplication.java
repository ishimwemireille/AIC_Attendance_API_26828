package rw.ac.auca.aicattendancesystem;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

/**
 * The Class AICAttendanceSystemApplication.
 *
 * The application uses two data stores, so each one is pointed at its own
 * packages: the transactional records live in PostgreSQL, while the append-only
 * notification log and audit trail live in MongoDB.
 *
 * @author Ishimwe Mireille
 * @version 1.0
 */
@SpringBootApplication
@ComponentScan(basePackages = "rw.ac.auca.*")
@EntityScan(basePackages = "rw.ac.auca.*")
@EnableJpaRepositories(basePackages = {
        "rw.ac.auca.student.repository",
        "rw.ac.auca.session.repository",
        "rw.ac.auca.attendance.repository",
        "rw.ac.auca.user.repository"
})
@EnableMongoRepositories(basePackages = {
        "rw.ac.auca.notification.repository",
        "rw.ac.auca.audit.repository"
})
public class AICAttendanceSystemApplication {

    public static void main(String[] args) {
        SpringApplication.run(AICAttendanceSystemApplication.class, args);
    }

}
