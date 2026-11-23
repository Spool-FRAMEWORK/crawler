package software.spool.crawler.api.builder;

import software.spool.crawler.internal.control.steps.PublishQuarantinedEnvelopeReplacedStep;
import software.spool.crawler.api.port.WhenQuarantined;
import software.spool.core.adapter.jackson.RecordSerializerFactory;
import software.spool.core.model.vo.IdempotencyKeySchema;
import software.spool.core.pipeline.ObservedStep;
import software.spool.core.pipeline.Pipeline;
import software.spool.core.pipeline.PipelineContext;
import software.spool.core.port.metrics.MetricsRegistry.LongHistogramMetric;
import software.spool.crawler.api.utils.CrawlerPorts;
import software.spool.crawler.internal.control.steps.BuildAndStoreEnvelopeStep;
import software.spool.crawler.internal.control.steps.BuildCapturedEventStep;
import software.spool.crawler.internal.control.steps.PayloadSizeMetricStep;
import software.spool.crawler.internal.control.steps.PublishCapturedEvent;
import software.spool.crawler.internal.control.steps.PublishDomainEventStep;
import software.spool.crawler.internal.control.steps.PublishEnvelopeStoredStep;

/**
 * The steps every record a crawler receives goes through, from a stream message or a webhook delivery: measure it,
 * build its captured event, publish its domain event and the captured one, store its envelope, publish that it took the
 * place of one in quarantine when it did, and publish that it was stored.
 */
final class CapturePipeline {
    private CapturePipeline() {
    }

    static Pipeline<PipelineContext, PipelineContext> of(EventMappingSpecification spec, CrawlerPorts ports,
                                                         IdempotencyKeySchema idempotencyKeySchema,
                                                         WhenQuarantined whenQuarantined,
                                                         LongHistogramMetric sizeHistogram) {
        return Pipeline.<PipelineContext>start()
                .add(new ObservedStep<>("measure-size", new PayloadSizeMetricStep(sizeHistogram)))
                .add(new ObservedStep<>("build-captured", new BuildCapturedEventStep(idempotencyKeySchema)))
                .add(new ObservedStep<>("emit-domain-event", new PublishDomainEventStep(spec.buildEmitter(ports.bus()))))
                .add(new ObservedStep<>("publish-captured", new PublishCapturedEvent(ports.trackedBus())))
                .add(new ObservedStep<>("store-envelope", new BuildAndStoreEnvelopeStep(ports.trackedInboxWriter(), RecordSerializerFactory.record(), spec.partitionAttributes(), whenQuarantined)))
                .add(new ObservedStep<>("publish-replaced", new PublishQuarantinedEnvelopeReplacedStep(ports.bus())))
                .add(new ObservedStep<>("publish-stored", new PublishEnvelopeStoredStep(ports.bus())));
    }
}
