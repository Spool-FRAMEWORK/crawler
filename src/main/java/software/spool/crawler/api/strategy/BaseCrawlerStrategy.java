package software.spool.crawler.api.strategy;

import software.spool.crawler.api.ErrorRouter;
import software.spool.crawler.api.exception.InboxWriteException;
import software.spool.crawler.api.exception.SourceOpenException;
import software.spool.crawler.api.exception.SourcePollException;
import software.spool.crawler.api.exception.SpoolException;
import software.spool.crawler.api.port.EventBus;
import software.spool.model.InboxFailed;
import software.spool.model.SourceFailed;

import java.util.Objects;

public class BaseCrawlerStrategy implements CrawlerStrategy {
        private final EventBus bus;
        private final String sender;
        protected final ErrorRouter errorRouter;

        public BaseCrawlerStrategy(EventBus bus, String sender, ErrorRouter errorRouter) {
                this.bus = bus;
                this.sender = sender;
                this.errorRouter = Objects.isNull(errorRouter) ? initializeErrorRouter() : errorRouter;
        }

        private ErrorRouter initializeErrorRouter() {
                return new ErrorRouter()
                                .on(InboxWriteException.class,
                                                e -> bus.emit(InboxFailed.from(sender).with(e.getMessage())))
                                .on(SourceOpenException.class, e -> bus.emit(SourceFailed.with(e.getMessage())))
                                .on(SourcePollException.class, e -> bus.emit(SourceFailed.with(e.getMessage())));
        }

        @Override
        public void execute() throws SpoolException {

        }
}
