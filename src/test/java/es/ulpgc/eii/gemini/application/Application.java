package es.ulpgc.eii.gemini.application;

import es.ulpgc.eii.gemini.application.crawler.GeminiTradeCrawlerSource;
import es.ulpgc.eii.spool.crawler.api.source.Inbox;
import es.ulpgc.eii.spool.crawler.api.strategy.CrawlerStrategy;
import es.ulpgc.eii.spool.crawler.dsl.Crawlers;
import es.ulpgc.eii.spool.crawler.internal.utils.Formats;
import es.ulpgc.eii.spool.crawler.internal.utils.JdbcInbox;

public class Application {
    private final CrawlerStrategy crawler;

    public Application() {
        Inbox inbox = new JdbcInbox();
        this.crawler = Crawlers.pull(new GeminiTradeCrawlerSource())
                .splitWith(Formats.JSON_ARRAY)
                .inbox(inbox)
                .create();
    }

    public void run() {
        crawler.execute();
    }
}
