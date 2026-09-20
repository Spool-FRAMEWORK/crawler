package software.spool.crawler.api.utils;

import software.spool.core.adapter.logging.LoggerFactory;
import software.spool.core.exception.DuplicateEventException;
import software.spool.core.port.bus.EventPublisher;
import software.spool.core.port.logging.Logger;
import software.spool.core.utils.routing.ErrorRouter;
import software.spool.crawler.api.Crawler;


public class CrawlerErrorRouter {

    private CrawlerErrorRouter() {
        // utility class
    }

    /**
     * Creates the default {@link ErrorRouter} wired to the given event bus.
     *
     * <p>A duplicate event is the inbox doing its job, so it is rejected without an error in the log.
     * Anything else is still logged as an error.</p>
     *
     * @param bus the event bus emitter used for publishing failure events
     * @return a pre-configured error router
     */
    public static ErrorRouter defaults(EventPublisher bus) {
        return defaults(bus, LoggerFactory.getLogger(Crawler.class));
    }

    /**
     * Same as {@link #defaults(EventPublisher)}, writing to the given logger.
     *
     * @param bus the event bus emitter used for publishing failure events
     * @param log where the routed errors are written
     * @return a pre-configured error router
     */
    public static ErrorRouter defaults(EventPublisher bus, Logger log) {
        return new ErrorRouter()
                .on(DuplicateEventException.class, (e, event) -> log.info("Rejected duplicate event {}", e.getIdempotencyKey()))
                .orElse((e, cause) -> log.error(e.getMessage()));
    }
}
