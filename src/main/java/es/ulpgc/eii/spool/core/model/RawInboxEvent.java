package es.ulpgc.eii.spool.core.model;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public record RawInboxEvent(
        String eventId,
        String eventType,
        Instant timestamp,
        String sourceId,
        Optional<String> payload
) implements SpoolEvent {

    public RawInboxEvent {
        if (sourceId == null || sourceId.isBlank()) {
            throw new IllegalArgumentException("sourceId is required");
        }
        if (eventType == null || eventType.isBlank()) {
            throw new IllegalArgumentException("eventType is required");
        }
        if (eventId == null || eventId.isBlank()) {
            throw new IllegalArgumentException("eventId is required");
        }
        if (timestamp == null) {
            throw new IllegalArgumentException("timestamp is required");
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private String sourceId;
        private Optional<String> payload = Optional.empty();

        public Builder sourceId(String sourceId) {
            this.sourceId = sourceId;
            return this;
        }

        public Builder payload(String payload) {
            this.payload = Optional.ofNullable(payload);
            return this;
        }

        public Builder payload(Optional<String> payload) {
            this.payload = payload;
            return this;
        }

        public RawInboxEvent build() {
            if (sourceId == null || sourceId.isBlank()) {
                throw new IllegalArgumentException("sourceId is required");
            }

            String eventId = UUID.randomUUID().toString();
            String eventType = "RAW_INBOX_ITEM";
            Instant timestamp = Instant.now();

            return new RawInboxEvent(
                    eventId,
                    eventType,
                    timestamp,
                    sourceId,
                    payload
            );
        }
    }
}
