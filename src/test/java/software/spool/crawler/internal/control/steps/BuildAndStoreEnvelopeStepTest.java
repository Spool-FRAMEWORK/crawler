package software.spool.crawler.internal.control.steps;

import org.junit.jupiter.api.Test;
import software.spool.core.exception.DuplicateEventException;
import software.spool.core.exception.InboxWriteException;
import software.spool.core.model.event.SourcePayloadCaptured;
import software.spool.core.model.vo.IdempotencyKey;
import software.spool.core.model.vo.MediaType;
import software.spool.core.pipeline.PipelineContext;
import software.spool.core.port.health.HealthStatus;
import software.spool.core.port.health.Tracked;
import software.spool.crawler.api.port.InboxWriter;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BuildAndStoreEnvelopeStepTest {

    private static final IdempotencyKey KEY = IdempotencyKey.of("source", "payload".getBytes());

    @Test
    void aDuplicateIsStillRejected() {
        Tracked<InboxWriter> writer = Tracked.of(envelope -> { throw new DuplicateEventException(KEY); }, "inbox-writer");

        assertThatThrownBy(() -> stepFor(writer).apply(context())).isInstanceOf(DuplicateEventException.class);
    }

    @Test
    void aDuplicateDoesNotMakeTheInboxWriterUnhealthy() {
        Tracked<InboxWriter> writer = Tracked.of(envelope -> { throw new DuplicateEventException(KEY); }, "inbox-writer");

        assertThatThrownBy(() -> stepFor(writer).apply(context())).isInstanceOf(DuplicateEventException.class);

        assertThat(writer.probe().status()).isEqualTo(HealthStatus.HEALTHY);
    }

    @Test
    void aDuplicateAnswerWithoutAKeyDoesNotMakeItUnhealthyEither() {
        Tracked<InboxWriter> writer = Tracked.of(envelope -> null, "inbox-writer");

        assertThatThrownBy(() -> stepFor(writer).apply(context())).isInstanceOf(DuplicateEventException.class);

        assertThat(writer.probe().status()).isEqualTo(HealthStatus.HEALTHY);
    }

    @Test
    void aDuplicateAfterARealFailureShowsThatTheWriterAnswersAgain() {
        Tracked<InboxWriter> writer = Tracked.of(envelope -> { throw new DuplicateEventException(KEY); }, "inbox-writer");
        writer.recordFailure("it was down");

        assertThatThrownBy(() -> stepFor(writer).apply(context())).isInstanceOf(DuplicateEventException.class);

        assertThat(writer.probe().status()).isEqualTo(HealthStatus.HEALTHY);
    }

    @Test
    void aRealFailureStillMakesTheInboxWriterUnhealthy() {
        Tracked<InboxWriter> writer = Tracked.of(envelope -> { throw new InboxWriteException("disk full"); }, "inbox-writer");

        assertThatThrownBy(() -> stepFor(writer).apply(context())).isInstanceOf(InboxWriteException.class);

        assertThat(writer.probe().status()).isEqualTo(HealthStatus.UNHEALTHY);
    }

    @Test
    void aStoredEnvelopeKeepsTheWriterHealthyAndKeepsItsKeyInTheContext() throws Exception {
        Tracked<InboxWriter> writer = Tracked.of(envelope -> KEY, "inbox-writer");

        PipelineContext result = stepFor(writer).apply(context());

        assertThat(writer.probe().status()).isEqualTo(HealthStatus.HEALTHY);
        assertThat(result.require(CapturedPayloadKeys.RECEIVED_KEY)).isEqualTo(KEY);
    }

    private static BuildAndStoreEnvelopeStep stepFor(Tracked<InboxWriter> writer) {
        return new BuildAndStoreEnvelopeStep(writer, record -> "schema".getBytes(), List.of("id"));
    }

    private static PipelineContext context() {
        return PipelineContext.empty()
                .with(CapturedPayloadKeys.SOURCE_ID, "source")
                .with(CapturedPayloadKeys.MEDIA_TYPE, MediaType.of("application/json"))
                .with(CapturedPayloadKeys.PAYLOAD, "payload".getBytes())
                .with(CapturedPayloadKeys.DOMAIN_MAPPING, Optional.empty())
                .with(CapturedPayloadKeys.CAPTURED_EVENT, SourcePayloadCaptured.builder()
                        .idempotencyKey(KEY)
                        .correlationId("corr-1")
                        .build());
    }
}
