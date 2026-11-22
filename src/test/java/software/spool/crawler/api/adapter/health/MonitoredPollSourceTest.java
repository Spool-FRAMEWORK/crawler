package software.spool.crawler.api.adapter.health;

import org.junit.jupiter.api.Test;
import software.spool.core.exception.PieceAsksToWaitException;
import software.spool.core.exception.SourcePollException;
import software.spool.core.port.health.HealthStatus;
import software.spool.core.port.health.PieceCheckPolicy;
import software.spool.core.port.health.PieceMonitor;
import software.spool.core.resilience.exception.CircuitBreakerOpenException;
import software.spool.crawler.api.port.source.PollSource;

import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MonitoredPollSourceTest {
    private final PieceMonitor monitor = PieceMonitor.of("orders-api", "HTTP", Optional.empty(), PieceCheckPolicy.defaults());

    @Test
    void fetch_whenTheSourceFails_marksThePieceUnhealthy() {
        PollSource<String> source = new MonitoredPollSource<>(sourceThat(() -> {
            throw new SourcePollException("orders-api", "HTTP 500");
        }), monitor);

        assertThatThrownBy(source::fetch).isInstanceOf(SourcePollException.class);
        assertThat(monitor.health().status()).isEqualTo(HealthStatus.UNHEALTHY);
    }

    @Test
    void fetch_whenTheSourceAsksToWait_isNotCalledAgainUntilThen() {
        AtomicInteger fetches = new AtomicInteger();
        PollSource<String> source = new MonitoredPollSource<>(sourceThat(() -> {
            fetches.incrementAndGet();
            throw new PieceAsksToWaitException("orders-api", Instant.now().plusSeconds(60));
        }), monitor);

        assertThatThrownBy(source::fetch).isInstanceOf(PieceAsksToWaitException.class);
        assertThatThrownBy(source::fetch).isInstanceOf(CircuitBreakerOpenException.class);
        assertThat(fetches).hasValue(1);
    }

    @Test
    void open_givesASourceThatIsStillWatched() {
        PollSource<String> source = new MonitoredPollSource<>(sourceThat(() -> {
            throw new SourcePollException("orders-api", "HTTP 500");
        }), monitor);

        assertThatThrownBy(() -> source.open().fetch()).isInstanceOf(SourcePollException.class);
        assertThat(monitor.health().status()).isEqualTo(HealthStatus.UNHEALTHY);
    }

    @Test
    void fetch_givesWhatTheSourceGave_withItsId() {
        PollSource<String> source = new MonitoredPollSource<>(sourceThat(() -> "orders"), monitor);

        assertThat(source.fetch()).isEqualTo("orders");
        assertThat(source.sourceId()).isEqualTo("orders-api");
    }

    private static PollSource<String> sourceThat(Fetch fetch) {
        return new PollSource<>() {
            @Override
            public String fetch() {
                return fetch.fetch();
            }

            @Override
            public String sourceId() {
                return "orders-api";
            }
        };
    }

    @FunctionalInterface
    private interface Fetch {
        String fetch();
    }
}
