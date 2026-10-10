package rw.ac.auca.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import rw.ac.auca.notification.domain.NotificationLog;
import rw.ac.auca.notification.domain.NotificationStatus;
import rw.ac.auca.notification.repository.NotificationLogRepository;

import java.util.List;

/**
 * The Class NotificationController. Shows what the system tried to send.
 *
 * @author Ishimwe Mireille
 * @version 1.0
 */
@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    @Autowired
    private NotificationLogRepository notificationLogRepository;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'COHORT_LEADER')")
    public List<NotificationLog> findAll() {
        return notificationLogRepository.findAllByOrderByCreatedAtDesc();
    }

    @GetMapping("/failed")
    @PreAuthorize("hasAnyRole('ADMIN', 'COHORT_LEADER')")
    public List<NotificationLog> findFailed() {
        return notificationLogRepository.findByStatusOrderByCreatedAtDesc(NotificationStatus.FAILED);
    }
}
