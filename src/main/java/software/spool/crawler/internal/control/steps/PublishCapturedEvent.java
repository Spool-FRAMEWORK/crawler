package software.spool.crawler.internal.control.steps;

import software.spool.core.model.event.SourcePayloadCaptured;
import software.spool.core.pipeline.PipelineContext;
import software.spool.core.pipeline.Step;
import software.spool.core.port.bus.EventPublisher;
import software.spool.core.port.health.Tracked;

import javax.management.AttributeNotFoundException;

public class PublishCapturedEvent implements Step<PipelineContext, PipelineContext> {
    private final Tracked<EventPublisher> trackedBus;

    public PublishCapturedEvent(Tracked<EventPublisher> trackedBus) {
        this.trackedBus = trackedBus;
    }

    @Override
    public PipelineContext apply(PipelineContext ctx) throws AttributeNotFoundException {
        SourcePayloadCaptured event = ctx.require(CapturedPayloadKeys.CAPTURED_EVENT);
        try {
            trackedBus.get().publish(event);
            trackedBus.recordSuccess();
        } catch (Exception e) {
            trackedBus.recordFailure(e.getMessage());
            throw e;
        }
        return ctx;
    }
}
