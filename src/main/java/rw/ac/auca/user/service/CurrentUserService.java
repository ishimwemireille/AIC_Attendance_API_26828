package rw.ac.auca.user.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;
import rw.ac.auca.exception.BusinessRuleException;
import rw.ac.auca.user.domain.RoleName;
import rw.ac.auca.user.domain.User;
import rw.ac.auca.user.repository.UserRepository;

import java.util.Optional;

/**
 * The Class CurrentUserService. Resolves the signed-in user to a User record,
 * so that every attendance record can name who took it.
 *
 * @author Ishimwe Mireille
 * @version 1.0
 */
@Service
public class CurrentUserService {

    @Autowired
    private UserRepository userRepository;

    public User require() {
        return find().orElseThrow(() -> new BusinessRuleException(
                "The signed-in account has no user record in this system."));
    }

    public Optional<User> find() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return Optional.empty();
        }
        String email = emailOf(authentication);
        return email == null ? Optional.empty() : userRepository.findByEmailIgnoreCase(email);
    }

    /** True when the caller is a trainer and nothing more, so must only see their own sessions. */
    public boolean isTrainerOnly() {
        return find()
                .map(user -> user.hasRole(RoleName.TRAINER)
                        && !user.hasRole(RoleName.ADMIN)
                        && !user.hasRole(RoleName.COHORT_LEADER))
                .orElse(false);
    }

    private String emailOf(Authentication authentication) {
        Object principal = authentication.getPrincipal();
        if (principal instanceof OidcUser oidcUser) {
            return oidcUser.getEmail();
        }
        if (principal instanceof UserDetails userDetails) {
            return userDetails.getUsername();
        }
        return authentication.getName();
    }
}
