package software.spool.crawler.internal.control.steps;

import org.junit.jupiter.api.Test;
import software.spool.core.model.event.SourcePayloadCaptured;
import software.spool.core.pipeline.PipelineContext;

import javax.management.AttributeNotFoundException;

import static org.assertj.core.api.Assertions.assertThat;

class BuildCapturedEventStepTest {

    @Test
    void apply_validPayload_buildsCapturedEventWithKey() throws AttributeNotFoundException {
        PipelineContext ctx = PipelineContext.empty()
            .with(CapturedPayloadKeys.SOURCE_ID, "source-1")
            .with(CapturedPayloadKeys.PAYLOAD, "{\"key\":\"val\"}".getBytes());

        PipelineContext result = new BuildCapturedEventStep().apply(ctx);

        SourcePayloadCaptured event = result.require(CapturedPayloadKeys.CAPTURED_EVENT);
        assertThat(event.idempotencyKey()).isNotNull();
    }

    @Test
    void apply_setsNonBlankCorrelationId() throws AttributeNotFoundException {
        PipelineContext ctx = PipelineContext.empty()
            .with(CapturedPayloadKeys.SOURCE_ID, "source-1")
            .with(CapturedPayloadKeys.PAYLOAD, "payload".getBytes());

        PipelineContext result = new BuildCapturedEventStep().apply(ctx);

        SourcePayloadCaptured event = result.require(CapturedPayloadKeys.CAPTURED_EVENT);
        assertThat(event.correlationId()).isNotBlank();
    }
}
