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
import rw.ac.auca.user.repository.UserRepository;

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
            ensureUser("trainer.web@auca.ac.rw", "Jean Nsengiyumva",
                    Set.of(RoleName.TRAINER));
            ensureUser("trainer.db@auca.ac.rw", "Alice Kalisa",
                    Set.of(RoleName.TRAINER));
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
