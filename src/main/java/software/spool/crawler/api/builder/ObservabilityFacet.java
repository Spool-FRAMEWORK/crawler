package software.spool.crawler.api.builder;

import software.spool.core.utils.routing.ErrorRouter;

public class ObservabilityFacet<I> extends CrawlerFacet<I> {

    ErrorRouter errorRouter;

    ObservabilityFacet(PollingCrawlerBuilder<I> parent) {
        super(parent);
    }

    public ObservabilityFacet<I> withErrorRouter(ErrorRouter errorRouter) {
        this.errorRouter = errorRouter;
        return this;
    }
}