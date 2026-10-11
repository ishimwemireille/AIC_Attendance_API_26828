package rw.ac.auca.system.service;

import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.QueueInformation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import rw.ac.auca.config.RabbitConfig;
import rw.ac.auca.outbox.domain.OutboxEvent;
import rw.ac.auca.outbox.domain.OutboxStatus;
import rw.ac.auca.outbox.repository.OutboxEventRepository;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The Class SystemHealthService. What an administrator needs that nobody else
 * does: whether each store is reachable, how deep the queues are, and whether
 * any alert is stuck.
 *
 * Every probe is wrapped, because the whole point of the page is to be readable
 * when something is down. A failing store reports itself as down rather than
 * taking the page with it.
 *
 * @author Ishimwe Mireille
 * @version 1.0
 */
@Service
public class SystemHealthService {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private AmqpAdmin amqpAdmin;

    @Autowired
    private OutboxEventRepository outboxEventRepository;

    /** The three stores, each reporting for itself. */
    public List<Map<String, Object>> stores() {
        return List.of(postgres(), mongo(), rabbit());
    }

    private Map<String, Object> postgres() {
        Map<String, Object> store = base("PostgreSQL", "Students, courses, sessions, attendance");
        try {
            String version = jdbcTemplate.queryForObject("SELECT version()", String.class);
            Map<String, Object> rows = new LinkedHashMap<>();
            for (String table : List.of("students", "courses", "sessions", "attendances", "users")) {
                rows.put(table, jdbcTemplate.queryForObject(
                        "SELECT count(*) FROM " + table, Long.class));
            }
            store.put("up", true);
            store.put("detail", version == null ? "" : version.split(",")[0]);
            store.put("counts", rows);
        } catch (Exception failure) {
            down(store, failure);
        }
        return store;
    }

    private Map<String, Object> mongo() {
        Map<String, Object> store = base("MongoDB", "Audit trail and notification log");
        try {
            Map<String, Object> counts = new LinkedHashMap<>();
            for (String collection : mongoTemplate.getCollectionNames()) {
                counts.put(collection, mongoTemplate.getCollection(collection)
                        .estimatedDocumentCount());
            }
            store.put("up", true);
            store.put("detail", "Database " + mongoTemplate.getDb().getName());
            store.put("counts", counts);
        } catch (Exception failure) {
            down(store, failure);
        }
        return store;
    }

    private Map<String, Object> rabbit() {
        Map<String, Object> store = base("RabbitMQ", "Absence alerts to email and SMS");
        try {
            Map<String, Object> depths = new LinkedHashMap<>();
            for (String queue : List.of(RabbitConfig.QUEUE_EMAIL,
                    RabbitConfig.QUEUE_SMS, RabbitConfig.QUEUE_DEAD)) {
                QueueInformation information = amqpAdmin.getQueueInfo(queue);
                depths.put(queue, information == null ? "missing"
                        : information.getMessageCount() + " waiting, "
                          + information.getConsumerCount() + " listening");
            }
            store.put("up", true);
            store.put("detail", "Exchange " + RabbitConfig.EXCHANGE);
            store.put("counts", depths);
        } catch (Exception failure) {
            down(store, failure);
        }
        return store;
    }

    /** Counts by status plus the most recent events, newest first. */
    public Map<String, Object> outbox() {
        Map<String, Object> summary = new LinkedHashMap<>();
        for (OutboxStatus status : OutboxStatus.values()) {
            summary.put(status.name().toLowerCase(), outboxEventRepository.countByStatus(status));
        }
        Map<String, Object> report = new LinkedHashMap<>();
        report.put("counts", summary);
        report.put("recent", outboxEventRepository.findTop50ByOrderByCreatedOnDesc());
        return report;
    }

    /**
     * Puts a stuck alert back in the queue. Clearing the attempts is what lets
     * the drainer pick it up again on its next pass.
     */
    public OutboxEvent retry(java.util.UUID id) {
        OutboxEvent event = outboxEventRepository.findById(id)
                .orElseThrow(() -> new rw.ac.auca.exception.ResourceNotFoundException(
                        "No outbox event with id " + id));
        if (event.getStatus() == OutboxStatus.SENT) {
            throw new rw.ac.auca.exception.BusinessRuleException(
                    "That alert was already delivered and cannot be sent again.");
        }
        event.setStatus(OutboxStatus.PENDING);
        event.setAttempts(0);
        event.setLastError(null);
        return outboxEventRepository.save(event);
    }

    private Map<String, Object> base(String name, String holds) {
        Map<String, Object> store = new LinkedHashMap<>();
        store.put("name", name);
        store.put("holds", holds);
        store.put("up", false);
        store.put("detail", "");
        store.put("counts", Map.of());
        return store;
    }

    private void down(Map<String, Object> store, Exception failure) {
        store.put("up", false);
        store.put("detail", failure.getMessage() == null
                ? failure.getClass().getSimpleName() : failure.getMessage());
    }
}
