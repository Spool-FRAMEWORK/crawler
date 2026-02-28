package software.spool.model;

import java.time.Instant;
import java.util.UUID;

public record RawDataWrittenToInbox(
        String eventId,
        Instant timestamp,
        String eventType,
        String sender,
        String idempotencyKey,
        String payload
) implements SpoolEvent {

    public static Builder from(String source) {
        return new Builder(source);
    }

    public static class Builder {
        private final String sender;
        private String payload;
        private String idempotencyKey;

        public Builder(String sender) {
            this.sender = sender;
        }

        public Builder withPayload(String payload) {
            this.payload = payload;
            return this;
        }

        public Builder withIdempotencyKey(String idempotencyKey) {
            this.idempotencyKey = idempotencyKey;
            return this;
        }

        public RawDataWrittenToInbox create() {
            return new RawDataWrittenToInbox(
                    UUID.randomUUID().toString(),
                    Instant.now(),
                    "DataWrittenToInbox",
                    sender,
                    idempotencyKey,
                    payload
            );
        }
    }
}
