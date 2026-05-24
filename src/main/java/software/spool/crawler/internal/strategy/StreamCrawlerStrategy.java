package software.spool.crawler.internal.strategy;

import software.spool.core.exception.SpoolException;
import software.spool.core.port.bus.Handler;
import software.spool.core.utils.polling.CancellationToken;
import software.spool.core.utils.routing.ErrorRouter;
import software.spool.crawler.api.port.source.StreamSource;
import software.spool.crawler.api.strategy.CrawlerStrategy;
import software.spool.crawler.internal.utils.factory.Normalizer;

public class StreamCrawlerStrategy<I> implements CrawlerStrategy {
    private final StreamSource<I> source;
    private final Normalizer<I> normalizer;
    private final ErrorRouter errorRouter;
    private final Handler<byte[]> payloadCapturedHandler;

    public StreamCrawlerStrategy(StreamSource<I> source, Normalizer<I> normalizer, ErrorRouter errorRouter, Handler<byte[]> payloadCapturedHandler) {
        this.source = source;
        this.normalizer = normalizer;
        this.errorRouter = errorRouter;
        this.payloadCapturedHandler = payloadCapturedHandler;
    }

    @Override
    public void execute(CancellationToken token) throws SpoolException {
        source.start(
                m -> {
                    if (token.isCancelled()) return;
                    normalizer.normalize(m)
                            .takeWhile(p -> token.isActive())
                            .forEach(payloadCapturedHandler::handle);
                },
                e -> {
                    if (token.isCancelled()) return;
                    errorRouter.dispatch(e);
                }
        );
    }
}
