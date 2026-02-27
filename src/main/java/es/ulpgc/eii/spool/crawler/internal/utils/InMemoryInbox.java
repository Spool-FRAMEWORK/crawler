package es.ulpgc.eii.spool.crawler.internal.utils;

import es.ulpgc.eii.spool.core.model.RawInboxEvent;
import es.ulpgc.eii.spool.crawler.api.exception.SpoolException;
import es.ulpgc.eii.spool.crawler.api.source.Inbox;
import es.ulpgc.eii.spool.crawler.api.source.InboxEntryId;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class InMemoryInbox implements Inbox {
    private final Map<UUID, RawInboxEvent> inbox;

    public InMemoryInbox() {
        inbox = new HashMap<>();
    }

    @Override
    public InboxEntryId receive(RawInboxEvent event) throws SpoolException {
        UUID uuid = UUID.randomUUID();
        this.inbox.put(uuid, event);
        return new InboxEntryId(uuid.toString());
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
