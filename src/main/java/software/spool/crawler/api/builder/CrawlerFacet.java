package software.spool.crawler.api.builder;

abstract class CrawlerFacet<I> {
    protected final PollingCrawlerBuilder<I> parent;

    protected CrawlerFacet(PollingCrawlerBuilder<I> parent) {
        this.parent = parent;
    }

    public PollingCrawlerBuilder<I> and() {
        return parent;
    }
}