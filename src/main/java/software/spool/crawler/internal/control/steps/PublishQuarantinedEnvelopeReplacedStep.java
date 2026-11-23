package software.spool.crawler.internal.control.steps;

import software.spool.core.model.event.QuarantinedEnvelopeReplaced;
import software.spool.core.pipeline.PipelineContext;
import software.spool.core.pipeline.Step;
import software.spool.core.port.bus.EventPublisher;

import javax.management.AttributeNotFoundException;

/**
 * Publishes that a captured envelope took the place of one in quarantine, so the janitor resolves its record as
 * replaced. A capture that replaced nothing passes through untouched.
 */
public class PublishQuarantinedEnvelopeReplacedStep implements Step<PipelineContext, PipelineContext> {
    private final EventPublisher publisher;

    public PublishQuarantinedEnvelopeReplacedStep(EventPublisher publisher) {
        this.publisher = publisher;
    }

    @Override
    public PipelineContext apply(PipelineContext ctx) throws AttributeNotFoundException {
        if (ctx.require(CapturedPayloadKeys.REPLACED_QUARANTINED)) {
            publisher.publish(QuarantinedEnvelopeReplaced.of(ctx.require(CapturedPayloadKeys.RECEIVED_KEY),
                    ctx.require(CapturedPayloadKeys.CAPTURED_EVENT).correlationId()));
        }
        return ctx;
    }
}
