package software.spool.crawler.internal.strategy;

import software.spool.core.exception.*;
import software.spool.core.adapter.logging.LoggerFactory;
import software.spool.core.port.bus.Handler;
import software.spool.core.port.logging.Logger;
import software.spool.core.resilience.exception.CircuitBreakerOpenException;
import software.spool.core.utils.polling.CancellationToken;
import software.spool.core.utils.polling.PollingConfiguration;
import software.spool.core.utils.routing.ErrorRouter;
import software.spool.crawler.api.strategy.CrawlerStrategy;
import software.spool.crawler.api.normalizer.Normalizer;
import software.spool.crawler.api.port.source.PollSource;

import java.time.Duration;
import java.util.Objects;

public class PollingCrawlerStrategy<I> implements CrawlerStrategy {
    private static final Logger LOG = LoggerFactory.getLogger(PollingCrawlerStrategy.class);
    private final PollSource<I> source;
    private final Normalizer<I> normalizer;
    private final ErrorRouter errorRouter;
    private final Handler<byte[]> itemmCapturedHandler;
    private final PollingConfiguration pollingConfiguration;

    public PollingCrawlerStrategy(PollSource<I> source, Normalizer<I> normalizer,
                                  Handler<byte[]> itemmCapturedHandler,
                                  PollingConfiguration pollingConfiguration, ErrorRouter errorRouter) {
        this.source = Objects.requireNonNull(source);
        this.normalizer = Objects.requireNonNull(normalizer);
        this.errorRouter = Objects.requireNonNull(errorRouter);

        this.itemmCapturedHandler = Objects.requireNonNull(itemmCapturedHandler);
        this.pollingConfiguration = Objects.requireNonNullElse(pollingConfiguration,
                PollingConfiguration.every(Duration.ofSeconds(10)));
    }

    @Override
    public void execute(CancellationToken token) throws SpoolException {
        pollingConfiguration.scheduler().schedule(
                () -> { try (PollSource<I> openedSource = this.source.open()) {
                        normalizer.normalize(openedSource.fetch())
                            .takeWhile(p -> token.isActive())
                            .forEach(itemmCapturedHandler::handle);
                    } catch (CircuitBreakerOpenException | PieceAsksToWaitException waiting) {
                        LOG.debug("Skipping the poll of {}: {}", source.sourceId(), waiting.getMessage());
                    } catch (Exception e) { errorRouter.dispatch(e); }
                },
                pollingConfiguration.schedule(),
                token
        );
    }
}
