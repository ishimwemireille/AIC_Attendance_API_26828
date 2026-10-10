package rw.ac.auca.user.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import rw.ac.auca.user.domain.User;
import rw.ac.auca.user.repository.UserRepository;

/**
 * The Class DevUserDetailsService. Development only.
 *
 * Google cannot redirect back to a machine that is not reachable, so the dev
 * profile also accepts HTTP Basic: the username is the user's AUCA email and
 * the password is one shared development secret from application.properties.
 * No per-user password is stored anywhere; in production only OAuth2 is active.
 *
 * @author Ishimwe Mireille
 * @version 1.0
 */
@Service
@Profile("dev")
public class DevUserDetailsService implements UserDetailsService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Value("${app.dev.password}")
    private String developmentPassword;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new UsernameNotFoundException("No user found for " + email));

        String[] authorities = user.getRoles().stream()
                .map(role -> "ROLE_" + role.getName())
                .toArray(String[]::new);

        return org.springframework.security.core.userdetails.User
                .withUsername(user.getEmail())
                .password(passwordEncoder.encode(developmentPassword))
                .authorities(authorities)
                .disabled(!user.isActive())
                .build();
    }
}
