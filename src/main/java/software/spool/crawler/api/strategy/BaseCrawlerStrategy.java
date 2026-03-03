package software.spool.crawler.api.strategy;

import software.spool.crawler.api.ErrorRouter;
import software.spool.core.exception.*;
import software.spool.crawler.api.port.EventBusEmitter;
import software.spool.core.model.*;

import java.util.Objects;

public class BaseCrawlerStrategy implements CrawlerStrategy {
        private final EventBusEmitter bus;
        private final String sender;
        protected final ErrorRouter errorRouter;
        private final String sourceId;

        public BaseCrawlerStrategy(EventBusEmitter bus, String sourceId, String sender, ErrorRouter errorRouter) {
                this.bus = bus;
                this.sourceId = sourceId;
                this.sender = sender;
                this.errorRouter = Objects.isNull(errorRouter) ? initializeErrorRouter() : errorRouter;
        }

        private ErrorRouter initializeErrorRouter() {
                return new ErrorRouter().on(SourceOpenException.class,
                        (e, cause) -> bus.emit(SourceFetchFailed.builder()
                                .senderId(sender)
                                .sourceId(sourceId)
                                .errorMessage(e.getMessage()).build())).on(SourcePollException.class,
                        (e, cause) -> bus.emit(SourceFetchFailed.builder()
                                .senderId(sender)
                                .sourceId(sourceId)
                                .errorMessage(e.getMessage()).build())).on(DeserializationException.class,
                        (e, cause) -> bus.emit(SourceItemCaptureFailed.builder()
                                .senderId(sender)
                                .sourceId(sourceId)
                                .errorMessage(e.getMessage()).build())).on(SourceSplitException.class,
                        (e, cause) -> bus.emit(SourceItemCaptureFailed.builder()
                                .senderId(sender)
                                .sourceId(sourceId)
                                .errorMessage(e.getMessage()).build())).on(SerializationException.class,
                        (e, cause) -> bus.emit(SourceItemCaptureFailed.builder()
                                .senderId(sender)
                                .sourceId(sourceId)
                                .errorMessage(e.getMessage()).build())).on(InboxWriteException.class,
                        (e, cause) -> bus.emit(InboxItemStoreFailed.builder()
                                .from(cause)
                                .sourceId(sourceId)
                                .senderId(sender)
                                .errorMessage(e.getMessage()).build()));
        }

        @Override
        public void execute() throws SpoolException {
        }
}
