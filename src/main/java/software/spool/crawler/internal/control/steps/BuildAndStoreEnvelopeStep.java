package software.spool.crawler.internal.control.steps;

import software.spool.core.exception.DuplicateEventException;
import software.spool.core.model.EnvelopeStatus;
import software.spool.core.model.vo.*;
import software.spool.core.pipeline.PipelineContext;
import software.spool.core.pipeline.Step;
import software.spool.core.port.health.Tracked;
import software.spool.core.port.serde.RecordSerializer;
import software.spool.crawler.api.port.InboxWriter;
import software.spool.crawler.internal.utils.TypedDomainMapping;

import javax.management.AttributeNotFoundException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

public class BuildAndStoreEnvelopeStep implements Step<PipelineContext, PipelineContext> {
    private final Tracked<InboxWriter> trackedInboxWriter;
    private final RecordSerializer<PartitionKeySchema> serializer;
    private final List<String> defaultPartitionAttributes;

    public BuildAndStoreEnvelopeStep(Tracked<InboxWriter> trackedInboxWriter, RecordSerializer<PartitionKeySchema> serializer, List<String> defaultPartitionAttributes) {
        this.trackedInboxWriter = trackedInboxWriter;
        this.serializer = serializer;
        this.defaultPartitionAttributes = defaultPartitionAttributes;
    }

    /**
     * Stores the envelope in the inbox and keeps its key in the context.
     *
     * <p>A duplicate is rethrown as {@link DuplicateEventException} but is not a failure of the writer: the inbox
     * answered and refused the envelope on purpose, so the tracked writer is recorded as working. Any other
     * exception is recorded as a failure.</p>
     */
    @Override
    public PipelineContext apply(PipelineContext ctx) throws AttributeNotFoundException {
        try {
            IdempotencyKey key = trackedInboxWriter.get().receive(buildEnvelope(ctx));
            if (key == null)
                throw new DuplicateEventException(ctx.require(CapturedPayloadKeys.CAPTURED_EVENT).idempotencyKey());
            trackedInboxWriter.recordSuccess();
            return ctx.with(CapturedPayloadKeys.RECEIVED_KEY, key);
        } catch (DuplicateEventException e) {
            trackedInboxWriter.recordSuccess();
            throw e;
        } catch (Exception e) {
            trackedInboxWriter.recordFailure(e.getMessage());
            throw e;
        }
    }

    private Envelope buildEnvelope(PipelineContext ctx) throws AttributeNotFoundException {
        return new Envelope(
                ctx.require(CapturedPayloadKeys.CAPTURED_EVENT).idempotencyKey(),
                buildMetadata(ctx),
                ctx.require(CapturedPayloadKeys.MEDIA_TYPE),
                ctx.require(CapturedPayloadKeys.PAYLOAD),
                EnvelopeStatus.CAPTURED, 0, Instant.now(), null);
    }

    private EventMetadata buildMetadata(PipelineContext ctx) throws AttributeNotFoundException {
        Optional<TypedDomainMapping> matched = ctx.require(CapturedPayloadKeys.DOMAIN_MAPPING);
        EventMetadata metadata = new EventMetadata()
                .set(EventMetadataKey.SOURCE, ctx.require(CapturedPayloadKeys.SOURCE_ID))
                .set(EventMetadataKey.PARTITION_SCHEMA, serializer.serialize(buildPartitionSchema(ctx)))
                .set(EventMetadataKey.CORRELATION_ID, ctx.require(CapturedPayloadKeys.CAPTURED_EVENT).correlationId());
        matched.map(TypedDomainMapping::targetType)
                .ifPresent(type -> metadata.set(EventMetadataKey.TYPE, type.toString()));
        return metadata;
    }

    private PartitionKeySchema buildPartitionSchema(PipelineContext ctx) throws AttributeNotFoundException {
        Optional<TypedDomainMapping> matched = ctx.require(CapturedPayloadKeys.DOMAIN_MAPPING);
        return PartitionKeySchema.of(
                ctx.require(CapturedPayloadKeys.SOURCE_ID),
                matched.map(TypedDomainMapping::targetType).orElse(null),
                matched.map(TypedDomainMapping::partitionAttributes).orElse(defaultPartitionAttributes));
    }
}
