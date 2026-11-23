package software.spool.crawler.internal.control.steps;

import org.junit.jupiter.api.Test;
import software.spool.core.exception.IdempotencyKeyException;
import software.spool.core.model.event.SourcePayloadCaptured;
import software.spool.core.model.vo.IdempotencyKey;
import software.spool.core.model.vo.IdempotencyKeySchema;
import software.spool.core.pipeline.PipelineContext;

import javax.management.AttributeNotFoundException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BuildCapturedEventStepTest {

    @Test
    void apply_validPayload_buildsCapturedEventWithKey() throws AttributeNotFoundException {
        PipelineContext ctx = PipelineContext.empty()
            .with(CapturedPayloadKeys.SOURCE_ID, "source-1")
            .with(CapturedPayloadKeys.PAYLOAD, "{\"key\":\"val\"}".getBytes());

        PipelineContext result = new BuildCapturedEventStep(IdempotencyKeySchema.wholeRecord()).apply(ctx);

        SourcePayloadCaptured event = result.require(CapturedPayloadKeys.CAPTURED_EVENT);
        assertThat(event.idempotencyKey()).isNotNull();
    }

    @Test
    void apply_setsNonBlankCorrelationId() throws AttributeNotFoundException {
        PipelineContext ctx = PipelineContext.empty()
            .with(CapturedPayloadKeys.SOURCE_ID, "source-1")
            .with(CapturedPayloadKeys.PAYLOAD, "payload".getBytes());

        PipelineContext result = new BuildCapturedEventStep(IdempotencyKeySchema.wholeRecord()).apply(ctx);

        SourcePayloadCaptured event = result.require(CapturedPayloadKeys.CAPTURED_EVENT);
        assertThat(event.correlationId()).isNotBlank();
    }

    @Test
    void apply_schemaWithFields_keysTheRecordByThoseFields() throws AttributeNotFoundException {
        BuildCapturedEventStep step = new BuildCapturedEventStep(IdempotencyKeySchema.of(List.of("id")));

        assertThat(keyOf(step, "{\"id\":\"a-1\",\"fetchedAt\":\"10:01\"}"))
                .isEqualTo(keyOf(step, "{\"id\":\"a-1\",\"fetchedAt\":\"10:02\"}"));
    }

    @Test
    void apply_recordWithoutAFieldOfTheSchema_isRejectedNamingTheField() {
        BuildCapturedEventStep step = new BuildCapturedEventStep(IdempotencyKeySchema.of(List.of("id")));

        assertThatThrownBy(() -> keyOf(step, "{\"title\":\"Ley 1/2026\"}"))
                .isInstanceOf(IdempotencyKeyException.class)
                .hasMessageContaining("id");
    }

    private static IdempotencyKey keyOf(BuildCapturedEventStep step, String payload) throws AttributeNotFoundException {
        PipelineContext ctx = PipelineContext.empty()
            .with(CapturedPayloadKeys.SOURCE_ID, "source-1")
            .with(CapturedPayloadKeys.PAYLOAD, payload.getBytes());
        SourcePayloadCaptured event = step.apply(ctx).require(CapturedPayloadKeys.CAPTURED_EVENT);
        return event.idempotencyKey();
    }
}
