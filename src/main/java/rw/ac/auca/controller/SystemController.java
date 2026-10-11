package rw.ac.auca.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import rw.ac.auca.outbox.domain.OutboxEvent;
import rw.ac.auca.system.service.SystemHealthService;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The Class SystemController. The technical view of the running system, which
 * only an administrator may see: the state of each store, the depth of each
 * queue, and any alert that has not gone out.
 *
 * @author Ishimwe Mireille
 * @version 1.0
 */
@RestController
@RequestMapping("/api/system")
public class SystemController {

    @Autowired
    private SystemHealthService systemHealthService;

    @GetMapping("/stores")
    @PreAuthorize("hasRole('ADMIN')")
    public List<Map<String, Object>> stores() {
        return systemHealthService.stores();
    }

    @GetMapping("/outbox")
    @PreAuthorize("hasRole('ADMIN')")
    public Map<String, Object> outbox() {
        return systemHealthService.outbox();
    }

    @PostMapping("/outbox/{id}/retry")
    @PreAuthorize("hasRole('ADMIN')")
    public OutboxEvent retry(@PathVariable UUID id) {
        return systemHealthService.retry(id);
    }
}
