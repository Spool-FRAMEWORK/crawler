package software.spool.crawler.api.strategy;

import software.spool.crawler.api.exception.SpoolException;

public interface CrawlerStrategy {
    void execute() throws SpoolException;
}
