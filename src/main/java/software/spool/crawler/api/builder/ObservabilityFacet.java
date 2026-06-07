package software.spool.crawler.api.builder;

import software.spool.core.port.metrics.MetricsRegistry;
import software.spool.core.utils.routing.ErrorRouter;

public class ObservabilityFacet<B> extends CrawlerFacet<B> {

    ErrorRouter errorRouter;
    MetricsRegistry metricsRegistry;

    ObservabilityFacet(B parent) {
        super(parent);
    }

    public ObservabilityFacet<B> withErrorRouter(ErrorRouter errorRouter) {
        this.errorRouter = errorRouter;
        return this;
    }

    public ObservabilityFacet<B> withMetrics(MetricsRegistry metricsRegistry) {
        this.metricsRegistry = metricsRegistry;
        return this;
    }
}