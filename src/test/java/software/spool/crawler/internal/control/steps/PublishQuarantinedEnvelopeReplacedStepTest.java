package software.spool.crawler.internal.control.steps;

import org.junit.jupiter.api.Test;
import software.spool.core.model.Event;
import software.spool.core.model.event.QuarantinedEnvelopeReplaced;
import software.spool.core.model.event.SourcePayloadCaptured;
import software.spool.core.model.vo.IdempotencyKey;
import software.spool.core.pipeline.PipelineContext;
import software.spool.core.port.bus.EventPublisher;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PublishQuarantinedEnvelopeReplacedStepTest {
    private static final IdempotencyKey KEY = IdempotencyKey.of("boe-a-1");

    private final List<Event> published = new ArrayList<>();
    private final EventPublisher publisher = new EventPublisher() {
        @Override
        public <E extends Event> void publish(E event) {
            published.add(event);
        }
    };

    @Test
    void anEnvelopeThatTookThePlaceOfOneInQuarantine_isAnnouncedWithItsKeyAndCorrelation() throws Exception {
        new PublishQuarantinedEnvelopeReplacedStep(publisher).apply(context(true));

        assertThat(published).singleElement().isInstanceOfSatisfying(QuarantinedEnvelopeReplaced.class, replaced -> {
            assertThat(replaced.idempotencyKey()).isEqualTo(KEY);
            assertThat(replaced.correlationId()).isEqualTo("corr-1");
        });
    }

    @Test
    void anEnvelopeThatReplacedNothing_isNotAnnounced() throws Exception {
        new PublishQuarantinedEnvelopeReplacedStep(publisher).apply(context(false));

        assertThat(published).isEmpty();
    }

    private static PipelineContext context(boolean replaced) {
        return PipelineContext.empty()
                .with(CapturedPayloadKeys.RECEIVED_KEY, KEY)
                .with(CapturedPayloadKeys.REPLACED_QUARANTINED, replaced)
                .with(CapturedPayloadKeys.CAPTURED_EVENT, SourcePayloadCaptured.builder()
                        .idempotencyKey(KEY)
                        .correlationId("corr-1")
                        .build());
    }
}
