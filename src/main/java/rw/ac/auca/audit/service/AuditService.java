package rw.ac.auca.audit.service;

import tools.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import rw.ac.auca.audit.domain.AuditEntry;
import rw.ac.auca.audit.repository.AuditEntryRepository;
import rw.ac.auca.user.domain.User;
import rw.ac.auca.user.service.CurrentUserService;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * The Class AuditService. Writes one document per change to MongoDB.
 *
 * Audit writing never fails the business operation: if Mongo is unavailable the
 * attendance record is still saved, and the failure is logged instead.
 *
 * @author Ishimwe Mireille
 * @version 1.0
 */
@Service
public class AuditService {

    public static final String CREATE = "CREATE";
    public static final String UPDATE = "UPDATE";
    public static final String DELETE = "DELETE";

    @Autowired
    private AuditEntryRepository auditEntryRepository;

    @Autowired
    private CurrentUserService currentUserService;

    @Autowired
    private ObjectMapper objectMapper;

    public void record(String entity, UUID entityId, String action, Object before, Object after) {
        try {
            AuditEntry entry = new AuditEntry();
            entry.setEntity(entity);
            entry.setEntityId(entityId == null ? null : entityId.toString());
            entry.setAction(action);
            entry.setActor(actor());
            entry.setBefore(snapshot(before));
            entry.setAfter(snapshot(after));
            auditEntryRepository.save(entry);
        } catch (Exception exception) {
            System.err.println("Audit entry could not be written: " + exception.getMessage());
        }
    }

    private Map<String, Object> actor() {
        Map<String, Object> actor = new LinkedHashMap<>();
        User user = currentUserService.find().orElse(null);
        if (user == null) {
            actor.put("userId", null);
            actor.put("email", "system");
            actor.put("roles", List.of());
            return actor;
        }
        actor.put("userId", user.getId().toString());
        actor.put("email", user.getEmail());
        actor.put("roles", user.getRoles().stream()
                .map(role -> role.getName().name())
                .sorted()
                .collect(Collectors.toList()));
        return actor;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> snapshot(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return objectMapper.convertValue(value, Map.class);
        } catch (Exception exception) {
            return Map.of("_unserialisable", String.valueOf(exception.getMessage()));
        }
    }

    public List<AuditEntry> historyOf(String entity, UUID entityId) {
        return auditEntryRepository.findByEntityAndEntityIdOrderByAtDesc(entity, entityId.toString());
    }
}
