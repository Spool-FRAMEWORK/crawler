package software.spool.crawler.api.dsl;

import software.spool.crawler.api.source.PullSource;
import software.spool.crawler.internal.utils.CrawlerPorts;
import software.spool.crawler.internal.utils.InMemoryInboxWriter;

public final class Crawlers {
    private Crawlers() {}

    public static <R> PullSourceStep<R, R, R> poll(PullSource<R> source) {
        return new PullSourceStep<>(source, CrawlerPorts.builder().bus(System.out::println).inbox(new InMemoryInboxWriter()).build());
    }
}
