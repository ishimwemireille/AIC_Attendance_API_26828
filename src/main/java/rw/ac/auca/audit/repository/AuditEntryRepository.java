package rw.ac.auca.audit.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import rw.ac.auca.audit.domain.AuditEntry;

import java.util.List;

/**
 * The Interface AuditEntryRepository.
 *
 * @author Ishimwe Mireille
 * @version 1.0
 */
@Repository
public interface AuditEntryRepository extends MongoRepository<AuditEntry, String> {

    List<AuditEntry> findByEntityAndEntityIdOrderByAtDesc(String entity, String entityId);

    List<AuditEntry> findAllByOrderByAtDesc(Pageable pageable);
}
