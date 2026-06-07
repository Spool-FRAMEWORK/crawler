package software.spool.crawler.internal.utils;

import software.spool.core.adapter.logging.LoggerFactory;
import software.spool.core.exception.DeserializationException;
import software.spool.core.exception.SerializationException;
import software.spool.core.model.Event;
import software.spool.core.model.vo.IdempotencyKey;
import software.spool.core.port.bus.EventPublisher;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

public class DomainEventEmitter {
    private final EventPublisher bus;
    private final List<TypedDomainMapping> domainMappings;

    public DomainEventEmitter(EventPublisher bus, List<TypedDomainMapping> domainMappings) {
        this.bus = bus;
        this.domainMappings = domainMappings;
    }

    public Optional<TypedDomainMapping> emit(byte[] payload, IdempotencyKey idempotencyKey) {
        if (domainMappings.isEmpty()) return Optional.empty();
        for (TypedDomainMapping typed : domainMappings) {
            try {
                Event event = typed.mapping().resolve(payload, idempotencyKey);
                bus.publish(event);
                return Optional.of(typed);
            } catch (DeserializationException | SerializationException ignored) {
                LoggerFactory.getLogger("CrawlerDomainEmitting").warn("Mapper {} failed: {}", typed.getClass().getSimpleName(), ignored.getMessage());
            }
        }
        throw new DeserializationException(new String(payload), "No matching domain event mapper found");
    }
}
