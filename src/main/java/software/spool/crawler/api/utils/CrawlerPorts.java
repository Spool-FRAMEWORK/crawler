package software.spool.crawler.api.utils;

import software.spool.core.port.bus.EventPublisher;
import software.spool.core.port.decorator.SafeEventPublisher;
import software.spool.core.port.health.HealthProbe;
import software.spool.core.port.health.Tracked;
import software.spool.crawler.api.port.InboxWriter;
import software.spool.crawler.internal.port.decorator.SafeInboxWriter;

import java.util.List;

public class CrawlerPorts {
    private final Tracked<InboxWriter> trackedInboxWriter;
    private final Tracked<EventPublisher> trackedBus;

    private CrawlerPorts(Builder builder) {
        this.trackedInboxWriter = Tracked.of(SafeInboxWriter.of(builder.inboxWriter), "inbox");
        this.trackedBus = Tracked.of(SafeEventPublisher.of(builder.bus), "event-bus");
    }

    public InboxWriter inboxWriter() {
        return trackedInboxWriter.get();
    }

    public EventPublisher bus() {
        return trackedBus.get();
    }

    public Tracked<InboxWriter> trackedInboxWriter() {
        return trackedInboxWriter;
    }

    public Tracked<EventPublisher> trackedBus() {
        return trackedBus;
    }

    public List<HealthProbe> healthProbes() {
        return List.of(trackedInboxWriter, trackedBus);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private InboxWriter inboxWriter;
        private EventPublisher bus;

        Builder() {
        }

        public Builder inbox(InboxWriter inboxWriter) {
            this.inboxWriter = inboxWriter;
            return this;
        }

        public Builder bus(EventPublisher bus) {
            this.bus = bus;
            return this;
        }

        public CrawlerPorts build() {
            return new CrawlerPorts(this);
        }
    }
}
