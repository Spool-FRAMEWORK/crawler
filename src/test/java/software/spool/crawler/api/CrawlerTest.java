package software.spool.crawler.api;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.spool.core.model.spool.SpoolNode;
import software.spool.core.port.health.HealthStatus;
import software.spool.core.port.watchdog.ModuleHeartBeat;
import software.spool.core.utils.routing.ErrorRouter;
import software.spool.crawler.api.fixture.CapturingCrawlerStrategy;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class CrawlerTest {

    private CapturingCrawlerStrategy strategy;
    private Crawler crawler;
    private SpoolNode.StartPermit permit;

    @BeforeEach
    void setUp() {
        strategy = new CapturingCrawlerStrategy();
        crawler = new Crawler(strategy, new ErrorRouter(), ModuleHeartBeat.NOOP, List.of());
        permit = mock(SpoolNode.StartPermit.class);
    }

    @Test
    void start_delegatesToStrategy() {
        crawler.start(permit);
        assertThat(strategy.wasExecuted()).isTrue();
    }

    @Test
    void start_idempotent_doesNotStartTwice() {
        crawler.start(permit);
        crawler.start(permit);
        assertThat(strategy.executeCount()).isEqualTo(1);
    }

    @Test
    void stop_afterStart_cancelsPreviousToken() {
        crawler.start(permit);
        var lastToken = strategy.lastToken();

        crawler.stop(permit);

        assertThat(lastToken.isActive()).isFalse();
    }

    @Test
    void checkHealth_whenStopped_returnsDegraded() {
        assertThat(crawler.checkHealth().status()).isEqualTo(HealthStatus.DEGRADED);
    }

    @Test
    void checkHealth_whenRunning_returnsNotDegraded() {
        crawler.start(permit);
        assertThat(crawler.checkHealth().status()).isNotEqualTo(HealthStatus.DEGRADED);
    }
}
