package software.spool.crawler.api.builder;

import software.spool.core.adapter.jackson.RecordSerializerFactory;
import software.spool.core.adapter.otel.OpenTelemetryMetricsRegistry;
import software.spool.core.pipeline.ObservedStep;
import software.spool.core.pipeline.Pipeline;
import software.spool.core.pipeline.PipelineContext;
import software.spool.core.port.bus.Handler;
import software.spool.core.port.metrics.MetricsRegistry;
import software.spool.core.utils.routing.ErrorRouter;
import software.spool.crawler.api.Crawler;
import software.spool.crawler.api.utils.CrawlerErrorRouter;
import software.spool.crawler.internal.control.PayloadCapturedHandler;
import software.spool.crawler.internal.control.steps.*;
import software.spool.crawler.internal.strategy.StreamCrawlerStrategy;
import software.spool.crawler.internal.utils.factory.Normalizer;

import java.util.Objects;

class StreamCrawlerAssembler<I> {
    private final StreamCrawlerBuilder<I> config;

    StreamCrawlerAssembler(StreamCrawlerBuilder<I> config) {
        this.config = config;
    }

    Crawler assemble(Normalizer<I> normalizer) {
        config.sourceFacet.validate();
        return new Crawler(initializeStrategy(normalizer, initHandler()), getErrorRouter(), config.heartBeat);
    }

    private StreamCrawlerStrategy<I> initializeStrategy(Normalizer<I> normalizer, Handler<byte[]> handler) {
        return new StreamCrawlerStrategy<>(config.source, normalizer, getErrorRouter(), handler);
    }

    private ErrorRouter getErrorRouter() {
        return Objects.requireNonNullElse(config.observabilityFacet.errorRouter, CrawlerErrorRouter.defaults(config.sourceFacet.ports.bus()));
    }

    private PayloadCapturedHandler initHandler() {
        return new PayloadCapturedHandler(initializePipeline(), config.source.sourceId(), config.sourceFacet.mediaType, getErrorRouter());
    }

    private Pipeline<PipelineContext, PipelineContext> initializePipeline() {
        EventMappingSpecification spec = config.mappingFacet.spec();
        return Pipeline.<PipelineContext>start()
                .add(new ObservedStep<>("measure-size", new PayloadSizeMetricStep(buildHistogram())))
                .add(new ObservedStep<>("build-captured", new BuildCapturedEventStep()))
                .add(new ObservedStep<>("emit-domain-event", new PublishDomainEventStep(spec.buildEmitter(config.sourceFacet.ports.bus()))))
                .add(new ObservedStep<>("publish-captured", new PublishCapturedEvent(config.sourceFacet.ports.bus())))
                .add(new ObservedStep<>("store-envelope", new BuildAndStoreEnvelopeStep(config.sourceFacet.ports.inboxWriter(), RecordSerializerFactory.record(), spec.partitionAttributes())))
                .add(new ObservedStep<>("publish-stored", new PublishEnvelopeStoredStep(config.sourceFacet.ports.bus())));
    }

    private MetricsRegistry.LongHistogramMetric buildHistogram() {
        return new OpenTelemetryMetricsRegistry()
                .histogram("spool.captured.payload.size", "", "By");
    }
}