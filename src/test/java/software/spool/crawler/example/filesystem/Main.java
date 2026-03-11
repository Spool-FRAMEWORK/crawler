package software.spool.crawler.example.filesystem;

import software.spool.core.adapter.InMemoryEventBus;
import software.spool.core.model.SourceFetchFailed;
import software.spool.crawler.api.utils.CrawlerErrorRouter;
import software.spool.crawler.api.utils.Formats;
import software.spool.crawler.api.builder.CrawlerBuilderFactory;
import software.spool.crawler.api.strategy.CrawlerStrategy;
import software.spool.crawler.internal.adapter.InMemoryInboxWriter;
import software.spool.crawler.api.utils.CrawlerPorts;

public class Main {
    public static void main(String[] args) {
        InMemoryInboxWriter inMemoryInboxWriter = new InMemoryInboxWriter();
        InMemoryEventBus bus = new InMemoryEventBus();
        bus.on(SourceFetchFailed.class, System.out::println);
        CrawlerStrategy products = CrawlerBuilderFactory.poll(new FileSystemSource())
                .withFormat(Formats.JSON_ARRAY)
                .ports(CrawlerPorts.builder()
                        .bus(bus)
                        .inbox(inMemoryInboxWriter)
                        .errorRouter(CrawlerErrorRouter.defaults(bus)).build()
                )
                .create();
        products.execute();
    }
}