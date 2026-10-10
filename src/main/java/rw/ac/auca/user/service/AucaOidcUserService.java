package rw.ac.auca.user.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;
import rw.ac.auca.user.domain.User;
import rw.ac.auca.user.repository.UserRepository;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * The Class AucaOidcUserService. Runs after Google has authenticated someone:
 * it refuses any address outside AUCA, refuses an account that has not been
 * granted access, and turns the user's roles into Spring Security authorities.
 *
 * @author Ishimwe Mireille
 * @version 1.0
 */
@Service
public class AucaOidcUserService extends OidcUserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserServiceImpl userService;

    @Override
    public OidcUser loadUser(OidcUserRequest userRequest) throws OAuth2AuthenticationException {
        OidcUser googleUser = super.loadUser(userRequest);
        String email = googleUser.getEmail() == null ? "" : googleUser.getEmail().toLowerCase();

        String allowedDomain = userService.getAllowedDomain();
        if (allowedDomain != null && !allowedDomain.isBlank()
                && !email.endsWith(allowedDomain.toLowerCase())) {
            throw new OAuth2AuthenticationException(new OAuth2Error("invalid_domain"),
                    "Only an " + allowedDomain + " account may sign in.");
        }

        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new OAuth2AuthenticationException(new OAuth2Error("no_access"),
                        "The account " + email + " has not been granted access. "
                                + "Ask an administrator to add you."));

        if (!user.isActive()) {
            throw new OAuth2AuthenticationException(new OAuth2Error("inactive"),
                    "The account " + email + " has been deactivated.");
        }

        // Remember the Google subject the first time this person signs in.
        if (user.getGoogleSubject() == null) {
            user.setGoogleSubject(googleUser.getSubject());
            userRepository.save(user);
        }

        Set<GrantedAuthority> authorities = user.getRoles().stream()
                .map(role -> (GrantedAuthority) new SimpleGrantedAuthority("ROLE_" + role.getName()))
                .collect(Collectors.toSet());

        return new DefaultOidcUser(authorities, googleUser.getIdToken(),
                googleUser.getUserInfo(), "email");
    }
}
