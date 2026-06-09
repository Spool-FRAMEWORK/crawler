package software.spool.crawler.internal.control.steps;

import org.junit.jupiter.api.Test;
import software.spool.core.model.event.SourcePayloadCaptured;
import software.spool.core.model.vo.IdempotencyKey;
import software.spool.core.pipeline.PipelineContext;
import software.spool.core.model.Event;
import software.spool.core.port.bus.EventPublisher;
import software.spool.crawler.internal.utils.DomainEventEmitter;
import software.spool.crawler.internal.utils.TypedDomainMapping;

import javax.management.AttributeNotFoundException;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class PublishDomainEventStepTest {

    @Test
    void apply_noMappings_storesDomainMappingEmpty() throws AttributeNotFoundException {
        PipelineContext ctx = PipelineContext.empty()
            .with(CapturedPayloadKeys.PAYLOAD, "payload".getBytes())
            .with(CapturedPayloadKeys.CAPTURED_EVENT, capturedEvent());
        DomainEventEmitter emitter = new DomainEventEmitter(new EventPublisher() {
            @Override public <E extends Event> void publish(E event) {}
        }, List.of());

        PipelineContext result = new PublishDomainEventStep(emitter).apply(ctx);

        Optional<TypedDomainMapping> mapping = result.require(CapturedPayloadKeys.DOMAIN_MAPPING);
        assertThat(mapping).isEmpty();
    }

    @Test
    void apply_domainMappingKeyPresentInContext() throws AttributeNotFoundException {
        PipelineContext ctx = PipelineContext.empty()
            .with(CapturedPayloadKeys.PAYLOAD, "payload".getBytes())
            .with(CapturedPayloadKeys.CAPTURED_EVENT, capturedEvent());
        DomainEventEmitter emitter = new DomainEventEmitter(new EventPublisher() {
            @Override public <E extends Event> void publish(E event) {}
        }, List.of());

        PipelineContext result = new PublishDomainEventStep(emitter).apply(ctx);

        assertThat(result.require(CapturedPayloadKeys.DOMAIN_MAPPING)).isNotNull();
    }

    private static SourcePayloadCaptured capturedEvent() {
        return SourcePayloadCaptured.builder()
            .idempotencyKey(IdempotencyKey.of("src", "payload".getBytes()))
            .correlationId("corr-1")
            .build();
    }
}
