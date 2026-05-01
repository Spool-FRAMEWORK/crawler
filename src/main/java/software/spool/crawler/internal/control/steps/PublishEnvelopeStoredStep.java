package software.spool.crawler.internal.control.steps;

import software.spool.core.model.event.EnvelopeStored;
import software.spool.core.pipeline.PipelineContext;
import software.spool.core.pipeline.Step;
import software.spool.core.port.bus.EventPublisher;

import javax.management.AttributeNotFoundException;

public class PublishEnvelopeStoredStep implements Step<PipelineContext, PipelineContext> {
    private final EventPublisher publisher;

    public PublishEnvelopeStoredStep(EventPublisher publisher) {
        this.publisher = publisher;
    }

    @Override
    public PipelineContext apply(PipelineContext ctx) throws AttributeNotFoundException {
        EnvelopeStored storedEvent = EnvelopeStored.builder()
                .from(ctx.require(CapturedPayloadKeys.CAPTURED_EVENT))
                .build();
        publisher.publish(storedEvent);
        return ctx;
    }
}