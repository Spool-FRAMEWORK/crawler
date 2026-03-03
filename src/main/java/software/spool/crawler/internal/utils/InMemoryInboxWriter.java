package software.spool.crawler.internal.utils;

import software.spool.core.exception.SpoolException;
import software.spool.crawler.api.port.InboxWriter;

import java.util.HashMap;
import java.util.Map;

public class InMemoryInboxWriter implements InboxWriter {
    private final Map<String, String> inbox;

    public InMemoryInboxWriter() {
        inbox = new HashMap<>();
    }

    @Override
    public String receive(String payload, String idempotencyKey) throws SpoolException {
        this.inbox.put(idempotencyKey, payload);
        return idempotencyKey;
    }

    @Override
    public String toString() {
        return "InMemoryInbox{" +
                buildString() +
                '}';
    }

    private String buildString() {
        StringBuilder builder = new StringBuilder();
        inbox.forEach((key, value) -> {
            builder.append(key);
            builder.append(": ");
            builder.append(value);
            builder.append("\n");
        });
        return builder.toString();
    }
}
