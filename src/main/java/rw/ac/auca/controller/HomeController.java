package rw.ac.auca.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The Class HomeController. Lists the API so the root address is not an error page.
 *
 * @author Ishimwe Mireille
 * @version 1.0
 */
@RestController
public class HomeController {

    @GetMapping("/")
    public Map<String, Object> index() {
        Map<String, Object> endpoints = new LinkedHashMap<>();
        endpoints.put("students", "/api/students");
        endpoints.put("sessions", "/api/sessions");
        endpoints.put("attendances", "/api/attendances");
        endpoints.put("register of a session", "/api/attendances/session/{sessionId}");
        endpoints.put("history of a student", "/api/attendances/student/{studentId}");

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("application", "AUCA Innovation Center - Student Attendance System");
        body.put("author", "Ishimwe Mireille (26828)");
        body.put("endpoints", endpoints);
        return body;
    }
}
