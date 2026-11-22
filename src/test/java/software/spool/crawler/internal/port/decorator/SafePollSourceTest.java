package software.spool.crawler.internal.port.decorator;

import org.junit.jupiter.api.Test;
import software.spool.core.exception.SourcePollException;
import software.spool.core.resilience.exception.CircuitBreakerOpenException;
import software.spool.crawler.api.port.source.PollSource;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SafePollSourceTest {

    @Test
    void fetch_whenTheSourceFails_wrapsTheFailureInSourcePollException() {
        PollSource<String> source = SafePollSource.of(sourceThatThrows(new IllegalStateException("HTTP 500")));

        assertThatThrownBy(source::fetch).isInstanceOf(SourcePollException.class).hasMessageContaining("HTTP 500");
    }

    @Test
    void fetch_whileTheCircuitOfTheSourceIsOpen_letsThatThroughAsItIs() {
        PollSource<String> source = SafePollSource.of(sourceThatThrows(new CircuitBreakerOpenException("orders-api")));

        assertThatThrownBy(source::fetch).isInstanceOf(CircuitBreakerOpenException.class);
    }

    private static PollSource<String> sourceThatThrows(RuntimeException failure) {
        return new PollSource<>() {
            @Override
            public String fetch() {
                throw failure;
            }

            @Override
            public String sourceId() {
                return "orders-api";
            }
        };
    }
}
