package rw.ac.auca.notification.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import rw.ac.auca.notification.domain.NotificationLog;
import rw.ac.auca.notification.domain.NotificationStatus;

import java.util.List;
import java.util.Optional;

/**
 * The Interface NotificationLogRepository.
 *
 * @author Ishimwe Mireille
 * @version 1.0
 */
@Repository
public interface NotificationLogRepository extends MongoRepository<NotificationLog, String> {

    Optional<NotificationLog> findByEventId(String eventId);

    boolean existsByEventId(String eventId);

    List<NotificationLog> findByStatusOrderByCreatedAtDesc(NotificationStatus status);

    List<NotificationLog> findAllByOrderByCreatedAtDesc();
}
