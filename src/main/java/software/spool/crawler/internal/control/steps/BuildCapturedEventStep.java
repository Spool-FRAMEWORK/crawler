package software.spool.crawler.internal.control.steps;

import software.spool.core.model.event.SourcePayloadCaptured;
import software.spool.core.model.vo.IdempotencyKeySchema;
import software.spool.core.pipeline.PipelineContext;
import software.spool.core.pipeline.Step;

import javax.management.AttributeNotFoundException;
import java.util.UUID;

public class BuildCapturedEventStep implements Step<PipelineContext, PipelineContext> {
    private final IdempotencyKeySchema idempotencyKeySchema;

    public BuildCapturedEventStep(IdempotencyKeySchema idempotencyKeySchema) {
        this.idempotencyKeySchema = idempotencyKeySchema;
    }

    @Override
    public PipelineContext apply(PipelineContext ctx) throws AttributeNotFoundException {
        SourcePayloadCaptured captured = SourcePayloadCaptured.builder()
                .idempotencyKey(idempotencyKeySchema.keyOf(ctx.require(CapturedPayloadKeys.SOURCE_ID),
                        ctx.require(CapturedPayloadKeys.PAYLOAD)))
                .correlationId(UUID.randomUUID().toString())
                .build();
        return ctx.with(CapturedPayloadKeys.CAPTURED_EVENT, captured);
    }
}
