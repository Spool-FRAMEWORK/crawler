package software.spool.crawler.internal.utils;

import software.spool.crawler.api.ErrorRouter;
import software.spool.crawler.api.port.EventBusEmitter;
import software.spool.crawler.api.port.InboxWriter;

public class CrawlerPorts {
    private final InboxWriter inboxWriter;
    private final EventBusEmitter bus;
    private final ErrorRouter errorRouter;

    private CrawlerPorts(Builder builder) {
        this.inboxWriter = builder.inboxWriter;
        this.bus = builder.bus;
        this.errorRouter = builder.errorRouter;
    }

    public InboxWriter inboxWriter() {
        return inboxWriter;
    }

    public EventBusEmitter bus() {
        return bus;
    }

    public ErrorRouter errorRouter() {
        return errorRouter;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private InboxWriter inboxWriter;
        private EventBusEmitter bus;
        private ErrorRouter errorRouter;

        public Builder inbox(InboxWriter inboxWriter) {
            this.inboxWriter = inboxWriter;
            return this;
        }

        public Builder bus(EventBusEmitter bus) {
            this.bus = bus;
            return this;
        }

        public Builder errorRouter(ErrorRouter errorRouter) {
            this.errorRouter = errorRouter;
            return this;
        }

        public CrawlerPorts build() {
            return new CrawlerPorts(this);
        }
    }
}
