package software.spool.crawler.internal.control;

import org.junit.jupiter.api.Test;
import software.spool.crawler.api.port.source.PollSource;

import static org.assertj.core.api.Assertions.assertThat;

class PollSourceReachabilityTest {

    @Test
    void run_closesTheSourceItOpens() throws Exception {
        CountingPollSource source = new CountingPollSource();
        PollSourceReachability reachability = new PollSourceReachability(source);

        reachability.run();
        reachability.run();
        reachability.run();

        assertThat(source.opened).isEqualTo(3);
        assertThat(source.closed).isEqualTo(3);
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
