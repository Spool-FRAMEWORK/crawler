package software.spool.crawler.api.dsl;

import software.spool.crawler.api.source.PollSource;
import software.spool.crawler.internal.utils.CrawlerPorts;
import software.spool.crawler.internal.utils.InMemoryInboxWriter;

public final class Crawlers {
    private Crawlers() {}

    public static <R> PollSourceStep<R, R, R> poll(PollSource<R> source) {
        return new PollSourceStep<>(source, CrawlerPorts.builder().bus(System.out::println).inbox(new InMemoryInboxWriter()).build());
    }
}
