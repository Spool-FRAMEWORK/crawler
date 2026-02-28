package software.spool.model;

import java.time.Instant;
import java.util.UUID;

public record SourceFailed(
        String eventId,
        String eventType,
        Instant timestamp,
        String errorMessage
) implements SpoolEvent {
    public static SourceFailed with(String errorMessage) {
        return new SourceFailed(UUID.randomUUID().toString(), "SOURCE_FAILED", Instant.now(), errorMessage);
    }
}
