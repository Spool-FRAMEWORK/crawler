package software.spool.crawler.internal.strategy;

import org.junit.jupiter.api.Test;
import software.spool.core.exception.PieceAsksToWaitException;
import software.spool.core.exception.SourcePollException;
import software.spool.core.resilience.exception.CircuitBreakerOpenException;
import software.spool.core.model.schedule.Schedule;
import software.spool.core.utils.polling.CancellationToken;
import software.spool.core.utils.polling.PollingConfiguration;
import software.spool.core.utils.routing.ErrorRouter;
import software.spool.crawler.api.port.source.PollSource;
import software.spool.crawler.api.normalizer.Normalizer;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PollingCrawlerStrategyTest {

    @Test
    @SuppressWarnings("unchecked")
    void execute_opensAndClosesTheSourceInEveryCycle() {
        CountingPollSource source = new CountingPollSource();
        Normalizer<String> normalizer = mock(Normalizer.class);
        when(normalizer.normalize(any())).thenReturn(Stream.empty());
        PollingConfiguration twoCycles = new PollingConfiguration(
                (task, schedule, token) -> {
                    task.run();
                    task.run();
                },
                Schedule.every(Duration.ofSeconds(1)));
        PollingCrawlerStrategy<String> strategy =
                new PollingCrawlerStrategy<>(source, normalizer, payload -> {}, twoCycles, new ErrorRouter());

        strategy.execute(CancellationToken.create());

        assertThat(source.opened).isEqualTo(2);
        assertThat(source.closed).isEqualTo(2);
    }

    @Test
    void execute_whenTheCircuitOfTheSourceIsOpen_skipsTheCycleWithoutRoutingAnError() {
        List<Exception> routed = cycleOf(() -> {
            throw new CircuitBreakerOpenException("orders-api");
        });

        assertThat(routed).isEmpty();
    }

    @Test
    void execute_whenTheSourceAsksToWait_doesNotRouteAnError() {
        List<Exception> routed = cycleOf(() -> {
            throw new PieceAsksToWaitException("orders-api", Instant.now().plusSeconds(3));
        });

        assertThat(routed).isEmpty();
    }

    @Test
    void execute_whenTheSourceFails_routesTheError() {
        List<Exception> routed = cycleOf(() -> {
            throw new SourcePollException("orders-api", "HTTP 500");
        });

        assertThat(routed).singleElement().isInstanceOf(SourcePollException.class);
    }

    @SuppressWarnings("unchecked")
    private static List<Exception> cycleOf(Supplier<String> fetch) {
        List<Exception> routed = new ArrayList<>();
        Normalizer<String> normalizer = mock(Normalizer.class);
        when(normalizer.normalize(any())).thenReturn(Stream.empty());
        PollingConfiguration oneCycle = new PollingConfiguration(
                (task, schedule, token) -> task.run(),
                Schedule.every(Duration.ofSeconds(1)));
        PollSource<String> source = new PollSource<>() {
            @Override
            public String fetch() {
                return fetch.get();
            }

            @Override
            public String sourceId() {
                return "orders-api";
            }
        };
        new PollingCrawlerStrategy<>(source, normalizer, payload -> {}, oneCycle,
                new ErrorRouter().orElse((exception, event) -> routed.add(exception)))
                .execute(CancellationToken.create());
        return routed;
    }

    private static final class CountingPollSource implements PollSource<String> {
        private int opened;
        private int closed;

        @Override
        public PollSource<String> open() {
            opened++;
            return this;
        }

        @Override
        public void close() {
            closed++;
        }

        @Override
        public String fetch() {
            return "";
        }

        @Override
        public String sourceId() {
            return "counting";
        }
    }
}
