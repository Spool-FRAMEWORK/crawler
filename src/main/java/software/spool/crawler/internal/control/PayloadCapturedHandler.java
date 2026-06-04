package software.spool.crawler.internal.control;

import software.spool.core.model.vo.MediaType;
import software.spool.core.pipeline.Pipeline;
import software.spool.core.pipeline.PipelineContext;
import software.spool.core.port.bus.Handler;
import software.spool.core.port.metrics.MetricsRegistry;
import software.spool.core.port.metrics.SpoolMetrics;
import software.spool.core.utils.routing.ErrorRouter;
import software.spool.crawler.internal.control.steps.CapturedPayloadKeys;

import java.util.Map;

public class PayloadCapturedHandler implements Handler<byte[]> {
    private final Pipeline<PipelineContext, PipelineContext> pipeline;
    private final String sourceId;
    private final MediaType mediaType;
    private final ErrorRouter errorRouter;
    private final MetricsRegistry.CounterMetric eventsCounter;
    private final MetricsRegistry.CounterMetric errorsCounter;
    private final MetricsRegistry.TimerMetric latencyTimer;

    public PayloadCapturedHandler(Pipeline<PipelineContext, PipelineContext> pipeline, String sourceId, MediaType mediaType, ErrorRouter errorRouter, MetricsRegistry.CounterMetric eventsCounter, MetricsRegistry.CounterMetric errorsCounter, MetricsRegistry.TimerMetric latencyTimer) {
        this.pipeline = pipeline;
        this.sourceId = sourceId;
        this.mediaType = mediaType;
        this.errorRouter = errorRouter;
        this.eventsCounter = eventsCounter;
        this.errorsCounter = errorsCounter;
        this.latencyTimer = latencyTimer;
    }

    @Override
    public void handle(byte[] payload) {
        PipelineContext initial = PipelineContext.empty()
                .with(CapturedPayloadKeys.SOURCE_ID, sourceId)
                .with(CapturedPayloadKeys.MEDIA_TYPE, mediaType)
                .with(CapturedPayloadKeys.PAYLOAD, payload);
        long start = System.nanoTime();
        pipeline.execute(initial)
                .peek(ctx -> {
                    long elapsed = (System.nanoTime() - start) / 1_000_000;
                    eventsCounter.increment(Map.of(SpoolMetrics.Attributes.SOURCE, sourceId, SpoolMetrics.Attributes.STATUS, "success"));
                    latencyTimer.record(elapsed, Map.of(SpoolMetrics.Attributes.SOURCE, sourceId));
                })
                .peekError(e -> {
                    long elapsed = (System.nanoTime() - start) / 1_000_000;
                    errorsCounter.increment(Map.of(SpoolMetrics.Attributes.SOURCE, sourceId));
                    latencyTimer.record(elapsed, Map.of(SpoolMetrics.Attributes.SOURCE, sourceId));
                    errorRouter.dispatch(e);
                });
    }
}
