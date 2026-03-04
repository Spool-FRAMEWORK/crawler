package software.spool.crawler.example.filesystem;

import software.spool.crawler.api.Formats;
import software.spool.crawler.api.dsl.Crawlers;
import software.spool.crawler.api.strategy.CrawlerStrategy;
import software.spool.crawler.internal.utils.InMemoryInboxWriter;

public class Main {
    public static void main(String[] args) {
        InMemoryInboxWriter inMemoryInboxWriter = new InMemoryInboxWriter();
        CrawlerStrategy products = Crawlers.poll(new FileSystemSource())
                .withFormat(Formats.JSON_ARRAY)
                .inbox(inMemoryInboxWriter)
                .senderName("Products")
                .create();
        products.execute();
        System.out.println(inMemoryInboxWriter);
    }
}
