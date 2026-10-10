package rw.ac.auca.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import rw.ac.auca.audit.domain.AuditEntry;
import rw.ac.auca.audit.repository.AuditEntryRepository;
import rw.ac.auca.audit.service.AuditService;

import java.util.List;
import java.util.UUID;

/**
 * The Class AuditController. Reading the audit trail is an ADMIN job.
 *
 * @author Ishimwe Mireille
 * @version 1.0
 */
@RestController
@RequestMapping("/api/audit")
public class AuditController {

    @Autowired
    private AuditEntryRepository auditEntryRepository;

    @Autowired
    private AuditService auditService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public List<AuditEntry> findRecent(@RequestParam(defaultValue = "50") int limit) {
        return auditEntryRepository.findAllByOrderByAtDesc(PageRequest.of(0, Math.min(limit, 200)));
    }

    @GetMapping("/{entity}/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public List<AuditEntry> findHistory(@PathVariable String entity, @PathVariable UUID id) {
        return auditService.historyOf(entity, id);
    }
}
