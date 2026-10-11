package rw.ac.auca.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.IndexOperations;
import org.springframework.data.mongodb.core.index.IndexResolver;
import org.springframework.data.mongodb.core.mapping.MongoMappingContext;
import rw.ac.auca.audit.domain.AuditEntry;
import rw.ac.auca.notification.domain.NotificationLog;

import java.util.List;

/**
 * The Class MongoIndexConfig.
 *
 * The indexes are declared on the documents themselves, with @Indexed and
 * @CompoundIndex. Spring Data does not act on those declarations unless it is
 * asked to, so without this they were documented and never created - and the
 * unique index on eventId is what actually makes a redelivered message
 * harmless. The check in NotificationConsumer alone is a race: two deliveries
 * handled at the same time both see nothing and both write.
 *
 * This reads the same annotations rather than restating them, so the document
 * class stays the single place an index is declared.
 *
 * @author Ishimwe Mireille
 * @version 1.0
 */
@Configuration
public class MongoIndexConfig {

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private MongoMappingContext mappingContext;

    @EventListener(ApplicationReadyEvent.class)
    public void createDeclaredIndexes() {
        IndexResolver resolver = IndexResolver.create(mappingContext);

        for (Class<?> document : List.of(NotificationLog.class, AuditEntry.class)) {
            IndexOperations operations = mongoTemplate.indexOps(document);
            resolver.resolveIndexFor(document).forEach(operations::createIndex);
        }
    }
}
