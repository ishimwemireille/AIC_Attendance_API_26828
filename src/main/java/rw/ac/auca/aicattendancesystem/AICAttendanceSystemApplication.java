package rw.ac.auca.aicattendancesystem;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * The Class AICAttendanceSystemApplication.
 *
 * @author Ishimwe Mireille
 * @version 1.0
 */
@SpringBootApplication
@ComponentScan(basePackages = "rw.ac.auca.*")
@EntityScan(basePackages = "rw.ac.auca.*")
@EnableJpaRepositories(basePackages = {"rw.ac.auca.*"})
public class AICAttendanceSystemApplication {

    public static void main(String[] args) {
        SpringApplication.run(AICAttendanceSystemApplication.class, args);
    }

}
