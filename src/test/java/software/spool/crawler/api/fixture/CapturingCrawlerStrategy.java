package software.spool.crawler.api.fixture;

import software.spool.core.exception.SpoolException;
import software.spool.core.utils.polling.CancellationToken;
import software.spool.crawler.api.strategy.CrawlerStrategy;

public class CapturingCrawlerStrategy implements CrawlerStrategy {
    private CancellationToken lastToken;
    private int executeCount;
    private int stopCount;

    @Override
    public void execute(CancellationToken token) throws SpoolException {
        this.lastToken = token;
        this.executeCount++;
    }

    @Override
    public void stop() {
        this.stopCount++;
    }

    public CancellationToken lastToken() {
        return lastToken;
    }

    public boolean wasExecuted() {
        return lastToken != null;
    }

    public int executeCount() {
        return executeCount;
    }

    public int stopCount() {
        return stopCount;
    }
}
