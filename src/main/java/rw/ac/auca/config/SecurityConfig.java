package rw.ac.auca.config;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import rw.ac.auca.user.service.AucaOidcUserService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * The Class SecurityConfig.
 *
 * Authentication is OAuth2: the user signs in with their AUCA Google account and
 * this application never stores a password. Authorisation is role based, and is
 * written on each controller method with @PreAuthorize.
 *
 * @author Ishimwe Mireille
 * @version 1.0
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Autowired
    private AucaOidcUserService oidcUserService;

    @Autowired
    private Environment environment;

    /** Absent until a Google client id and secret are configured. */
    @Autowired
    private ObjectProvider<ClientRegistrationRepository> clientRegistrations;

    /**
     * Where to send the browser once Google has vouched for the user. Google
     * redirects to this application's own port, which in development is not
     * where the React app is being served from, so the two are told apart by a
     * property. In production the front end is served from here and "/" is right.
     */
    @Value("${app.frontend-url:/}")
    private String frontendUrl;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(Customizer.withDefaults())
                .authorizeHttpRequests(requests -> requests
                        .requestMatchers("/", "/error", "/login/**", "/oauth2/**").permitAll()
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .anyRequest().authenticated());

        // Only wire Google in when a client registration actually exists, so that the
        // application still starts on a machine where the credentials are not set.
        if (clientRegistrations.getIfAvailable() != null) {
            http.oauth2Login(login -> login
                    .userInfoEndpoint(userInfo -> userInfo.oidcUserService(oidcUserService))
                    .defaultSuccessUrl(frontendUrl, true)
                    .failureHandler(this::sendBackWithTheReason));
        }

        // Development convenience only. See DevUserDetailsService.
        if (environment.matchesProfiles("dev")) {
            http.httpBasic(Customizer.withDefaults());
        }

        return http.build();
    }

    /**
     * AucaOidcUserService refuses a sign-in for one of three reasons, each with
     * its own code. Spring's own failure page calls all of them "Invalid
     * credentials", which is both wrong and unhelpful: the credentials were
     * accepted by Google. The code is carried back to the sign-in screen so the
     * person is told what actually happened.
     */
    private void sendBackWithTheReason(HttpServletRequest request, HttpServletResponse response,
                                       AuthenticationException failure) throws IOException {
        String code = failure instanceof OAuth2AuthenticationException oauth2Failure
                ? oauth2Failure.getError().getErrorCode() : "failed";

        response.sendRedirect(frontendUrl + (frontendUrl.contains("?") ? "&" : "?")
                + "error=" + URLEncoder.encode(code, StandardCharsets.UTF_8));
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /** The React front end runs on its own port during development. */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of("http://localhost:5173", "http://localhost:4173"));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
