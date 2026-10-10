package rw.ac.auca.audit.domain;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * The Class AuditEntry. A MongoDB document recording who changed what.
 *
 * "before" and "after" hold whatever the entity looked like, so their shape
 * differs per entity. A fixed set of columns would be mostly null.
 *
 * @author Ishimwe Mireille
 * @version 1.0
 */
@Getter
@Setter
@Document(collection = "audit_trail")
@CompoundIndex(name = "idx_entity_history", def = "{'entity': 1, 'entityId': 1, 'at': -1}")
@CompoundIndex(name = "idx_actor_history", def = "{'actor.userId': 1, 'at': -1}")
public class AuditEntry {

    @Id
    private String id;

    /** Student, Session, Attendance or User. */
    private String entity;

    /** The primary key of the row in PostgreSQL. */
    private String entityId;

    /** CREATE, UPDATE or DELETE. */
    private String action;

    /** { userId, email, roles } */
    private Map<String, Object> actor;

    /** Null on CREATE. */
    private Map<String, Object> before;

    /** Null on DELETE. */
    private Map<String, Object> after;

    private LocalDateTime at = LocalDateTime.now();
}
