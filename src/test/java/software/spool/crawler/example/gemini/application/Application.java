package software.spool.crawler.example.gemini.application;

import software.spool.crawler.example.gemini.application.crawler.GeminiTradeCrawlerSource;
import software.spool.crawler.api.port.InboxWriter;
import software.spool.crawler.api.strategy.CrawlerStrategy;
import software.spool.crawler.api.dsl.Crawlers;
import software.spool.crawler.api.Formats;

public class Application {
    private final CrawlerStrategy crawler;

    public Application() {
        this.crawler = Crawlers.poll(new GeminiTradeCrawlerSource())
                .withFormat(Formats.JSON_ARRAY)
                .senderName("GeminiHTTP")
                .create();
    }

    public void run() {
        crawler.execute();
    }
}
