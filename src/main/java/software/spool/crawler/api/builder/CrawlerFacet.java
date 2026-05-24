package software.spool.crawler.api.builder;

abstract class CrawlerFacet<B> {

    protected final B parent;

    protected CrawlerFacet(B parent) {
        this.parent = parent;
    }

    public B and() {
        return parent;
    }
}