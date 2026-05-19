package software.spool.crawler.internal.control.steps;

import software.spool.core.pipeline.PipelineContext;
import software.spool.core.pipeline.Step;
import software.spool.core.port.metrics.MetricsRegistry;

import javax.management.AttributeNotFoundException;
import java.util.Map;

public class PayloadSizeMetricStep implements Step<PipelineContext, PipelineContext> {
    private final MetricsRegistry.LongHistogramMetric histogram;

    public PayloadSizeMetricStep(MetricsRegistry.LongHistogramMetric histogram) {
        this.histogram = histogram;
    }

    @Override
    public PipelineContext apply(PipelineContext ctx) throws AttributeNotFoundException {
        byte[] payload = ctx.require(CapturedPayloadKeys.PAYLOAD);
        histogram.record(payload.length, Map.of());
        return ctx;
    }
}
