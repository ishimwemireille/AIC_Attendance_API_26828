package rw.ac.auca.outbox.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import rw.ac.auca.outbox.domain.OutboxEvent;
import rw.ac.auca.outbox.domain.OutboxStatus;

import java.util.List;
import java.util.UUID;

/**
 * The Interface OutboxEventRepository.
 *
 * @author Ishimwe Mireille
 * @version 1.0
 */
@Repository
public interface OutboxEventRepository extends JpaRepository<OutboxEvent, UUID> {

    List<OutboxEvent> findTop100ByStatusOrderByCreatedOnAsc(OutboxStatus status);

    long countByStatus(OutboxStatus status);
}
