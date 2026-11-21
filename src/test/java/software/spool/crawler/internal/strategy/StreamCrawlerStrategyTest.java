package software.spool.crawler.internal.strategy;

import org.junit.jupiter.api.Test;
import software.spool.core.port.bus.Handler;
import software.spool.core.utils.polling.CancellationToken;
import software.spool.core.utils.routing.ErrorRouter;
import software.spool.crawler.api.port.source.StreamSource;
import software.spool.crawler.api.normalizer.Normalizer;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class StreamCrawlerStrategyTest {

    @Test
    void stop_stopsTheStreamSourceOnce() {
        RecordingStreamSource source = new RecordingStreamSource();
        StreamCrawlerStrategy<String> strategy = strategyOver(source, new ArrayList<>());
        strategy.execute(CancellationToken.create());

        strategy.stop();

        assertThat(source.stopCount).isEqualTo(1);
    }

    @Test
    void stop_afterTheTokenIsCancelled_noLaterMessageReachesTheHandler() {
        RecordingStreamSource source = new RecordingStreamSource();
        List<byte[]> handled = new ArrayList<>();
        StreamCrawlerStrategy<String> strategy = strategyOver(source, handled);
        CancellationToken token = CancellationToken.create();
        strategy.execute(token);

        token.cancel();
        strategy.stop();
        source.deliver("late message");

        assertThat(handled).isEmpty();
    }

    @SuppressWarnings("unchecked")
    private static StreamCrawlerStrategy<String> strategyOver(RecordingStreamSource source, List<byte[]> handled) {
        Normalizer<String> normalizer = mock(Normalizer.class);
        when(normalizer.normalize(any())).thenAnswer(invocation -> Stream.of(((String) invocation.getArgument(0)).getBytes()));
        Handler<byte[]> handler = handled::add;
        return new StreamCrawlerStrategy<>(source, normalizer, new ErrorRouter(), handler);
    }

    private static final class RecordingStreamSource implements StreamSource<String> {
        private Consumer<String> onMessage = message -> {};
        private int stopCount;

        @Override
        public void start(Consumer<String> onMessage, Consumer<Exception> onError) {
            this.onMessage = onMessage;
        }

        @Override
        public void stop() {
            stopCount++;
        }

        @Override
        public String sourceId() {
            return "recording";
        }

        @Override
        public String fetch() {
            throw new UnsupportedOperationException("A stream source is not fetched");
        }

        void deliver(String message) {
            onMessage.accept(message);
        }
    }
}
