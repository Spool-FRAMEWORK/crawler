package software.spool.crawler.api.builder;

import software.spool.core.adapter.jackson.RecordSerializerFactory;
import software.spool.core.pipeline.ObservedStep;
import software.spool.core.pipeline.Pipeline;
import software.spool.core.pipeline.PipelineContext;
import software.spool.core.port.bus.Handler;
import software.spool.core.port.health.HealthProbe;
import software.spool.core.port.metrics.MetricsRegistry;
import software.spool.core.port.metrics.MetricsRegistry.CounterMetric;
import software.spool.core.port.metrics.MetricsRegistry.TimerMetric;
import software.spool.core.port.metrics.MetricsRegistry.LongHistogramMetric;
import software.spool.core.port.metrics.SpoolMetrics;
import software.spool.core.utils.routing.ErrorRouter;
import software.spool.crawler.api.Crawler;
import software.spool.crawler.api.utils.CrawlerErrorRouter;
import software.spool.crawler.internal.control.PayloadCapturedHandler;
import software.spool.crawler.internal.control.steps.*;
import software.spool.crawler.internal.strategy.StreamCrawlerStrategy;
import software.spool.crawler.internal.utils.factory.Normalizer;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

class StreamCrawlerAssembler<I> {
    private final StreamCrawlerBuilder<I> config;

    StreamCrawlerAssembler(StreamCrawlerBuilder<I> config) {
        this.config = config;
    }

    Crawler assemble(Normalizer<I> normalizer) {
        config.sourceFacet.validate();
        MetricsRegistry metrics = Objects.requireNonNullElse(config.observabilityFacet.metricsRegistry, MetricsRegistry.NOOP);
        String sourceId = config.source.sourceId();
        CounterMetric eventsCounter = metrics.counter(SpoolMetrics.named(SpoolMetrics.Crawler.EVENTS_TOTAL, sourceId), SpoolMetrics.Crawler.EVENTS_TOTAL_DESC, "events");
        CounterMetric errorsCounter = metrics.counter(SpoolMetrics.named(SpoolMetrics.Crawler.ERRORS_TOTAL, sourceId), SpoolMetrics.Crawler.ERRORS_TOTAL_DESC, "errors");
        TimerMetric latencyTimer = metrics.timer(SpoolMetrics.named(SpoolMetrics.Crawler.LATENCY, sourceId), SpoolMetrics.Crawler.LATENCY_DESC, "ms");
        LongHistogramMetric sizeHistogram = metrics.histogram(SpoolMetrics.named("spool.crawler.payload.size", sourceId), "", "By");
        List<HealthProbe> probes = new ArrayList<>(config.sourceFacet.ports.healthProbes());
        Handler<byte[]> handler = initHandler(eventsCounter, errorsCounter, latencyTimer, sizeHistogram);
        return new Crawler(initializeStrategy(normalizer, handler), getErrorRouter(), config.heartBeat, probes);
    }

    private StreamCrawlerStrategy<I> initializeStrategy(Normalizer<I> normalizer, Handler<byte[]> handler) {
        return new StreamCrawlerStrategy<>(config.source, normalizer, getErrorRouter(), handler);
    }

    private ErrorRouter getErrorRouter() {
        return Objects.requireNonNullElse(config.observabilityFacet.errorRouter, CrawlerErrorRouter.defaults(config.sourceFacet.ports.bus()));
    }

    private PayloadCapturedHandler initHandler(CounterMetric eventsCounter, CounterMetric errorsCounter, TimerMetric latencyTimer, LongHistogramMetric sizeHistogram) {
        return new PayloadCapturedHandler(initializePipeline(sizeHistogram), config.source.sourceId(), config.sourceFacet.mediaType, getErrorRouter(), eventsCounter, errorsCounter, latencyTimer);
    }

    private Pipeline<PipelineContext, PipelineContext> initializePipeline(LongHistogramMetric sizeHistogram) {
        EventMappingSpecification spec = config.mappingFacet.spec();
        return Pipeline.<PipelineContext>start()
                .add(new ObservedStep<>("measure-size", new PayloadSizeMetricStep(sizeHistogram)))
                .add(new ObservedStep<>("build-captured", new BuildCapturedEventStep()))
                .add(new ObservedStep<>("emit-domain-event", new PublishDomainEventStep(spec.buildEmitter(config.sourceFacet.ports.bus()))))
                .add(new ObservedStep<>("publish-captured", new PublishCapturedEvent(config.sourceFacet.ports.trackedBus())))
                .add(new ObservedStep<>("store-envelope", new BuildAndStoreEnvelopeStep(config.sourceFacet.ports.trackedInboxWriter(), RecordSerializerFactory.record(), spec.partitionAttributes())))
                .add(new ObservedStep<>("publish-stored", new PublishEnvelopeStoredStep(config.sourceFacet.ports.bus())));
    }
}
