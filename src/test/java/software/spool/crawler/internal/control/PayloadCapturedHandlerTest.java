package software.spool.crawler.internal.control;

import org.junit.jupiter.api.Test;
import software.spool.core.exception.DuplicateEventException;
import software.spool.core.model.vo.IdempotencyKey;
import software.spool.core.model.vo.MediaType;
import software.spool.core.pipeline.Pipeline;
import software.spool.core.pipeline.PipelineContext;
import software.spool.core.port.metrics.MetricsRegistry;
import software.spool.core.utils.routing.ErrorRouter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class PayloadCapturedHandlerTest {

    private final List<Map<String, String>> events = new ArrayList<>();
    private final List<Map<String, String>> errors = new ArrayList<>();
    private final List<Long> latencies = new ArrayList<>();
    private final List<Exception> routed = new ArrayList<>();

    private PayloadCapturedHandler handlerThatEndsWith(RuntimeException failure) {
        Pipeline<PipelineContext, PipelineContext> pipeline = Pipeline.<PipelineContext>start().add(context -> {
            if (failure != null) throw failure;
            return context;
        });
        MetricsRegistry.TimerMetric latency = new MetricsRegistry.TimerMetric() {
            @Override public void record(long durationMs, Map<String, String> attributes) { latencies.add(durationMs); }
            @Override public <T> T record(Map<String, String> attributes, MetricsRegistry.CheckedSupplier<T> supplier) { throw new UnsupportedOperationException(); }
            @Override public void record(Map<String, String> attributes, MetricsRegistry.CheckedRunnable runnable) { throw new UnsupportedOperationException(); }
        };
        return new PayloadCapturedHandler(pipeline, "source-1", MediaType.of("application/json"),
                new ErrorRouter().orElse((exception, event) -> routed.add(exception)),
                (value, attributes) -> events.add(attributes),
                (value, attributes) -> errors.add(attributes),
                latency);
    }

    private static DuplicateEventException duplicate() {
        return new DuplicateEventException(IdempotencyKey.of("source-1", "payload".getBytes()));
    }

    @Test
    void aDuplicateIsCountedAsADuplicateAndNotAsAnError() {
        handlerThatEndsWith(duplicate()).handle("payload".getBytes());

        assertThat(events).containsExactly(Map.of("source", "source-1", "status", "duplicate"));
        assertThat(errors).isEmpty();
    }

    @Test
    void aDuplicateIsStillRoutedAndItsLatencyRecorded() {
        DuplicateEventException duplicate = duplicate();

        handlerThatEndsWith(duplicate).handle("payload".getBytes());

        assertThat(routed).containsExactly(duplicate);
        assertThat(latencies).hasSize(1);
    }

    @Test
    void aRealFailureIsStillCountedAsAnError() {
        IllegalStateException failure = new IllegalStateException("the inbox is down");

        handlerThatEndsWith(failure).handle("payload".getBytes());

        assertThat(events).containsExactly(Map.of("source", "source-1", "status", "error"));
        assertThat(errors).containsExactly(Map.of("source", "source-1"));
        assertThat(routed).containsExactly(failure);
    }

    @Test
    void aStoredEventIsCountedAsASuccessAndNothingIsRouted() {
        handlerThatEndsWith(null).handle("payload".getBytes());

        assertThat(events).containsExactly(Map.of("source", "source-1", "status", "success"));
        assertThat(errors).isEmpty();
        assertThat(routed).isEmpty();
    }
}
