package software.spool.crawler.api.builder;

import software.spool.core.port.watchdog.ModuleHeartBeat;
import software.spool.crawler.api.Crawler;
import software.spool.crawler.api.normalizer.Normalizer;
import software.spool.core.port.serde.NamingConvention;
import software.spool.crawler.api.utils.NormalizerFormat;

/**
 * Builds a crawler that receives its records through a webhook on the node's HTTP server, authenticated (ADR-013),
 * instead of asking a source for them.
 */
public class WebhookCrawlerBuilder {
    final String sourceId;
    final ModuleHeartBeat heartBeat;
    final WebhookSourceFacet sourceFacet;
    final MappingFacet<WebhookCrawlerBuilder> mappingFacet;
    final ObservabilityFacet<WebhookCrawlerBuilder> observabilityFacet;

    public WebhookCrawlerBuilder(String sourceId, ModuleHeartBeat heartBeat) {
        this.sourceId = sourceId;
        this.heartBeat = heartBeat;
        this.sourceFacet = new WebhookSourceFacet(this);
        this.mappingFacet = new MappingFacet<>(this, new EventMappingSpecification(NamingConvention.SNAKE_CASE));
        this.observabilityFacet = new ObservabilityFacet<>(this);
    }

    public WebhookSourceFacet source() {
        return sourceFacet;
    }

    public MappingFacet<WebhookCrawlerBuilder> mapping() {
        return mappingFacet;
    }

    public ObservabilityFacet<WebhookCrawlerBuilder> observability() {
        return observabilityFacet;
    }

    public Crawler createWith(Normalizer<byte[]> normalizer) {
        return new WebhookCrawlerAssembler(this).assemble(normalizer);
    }

    public Crawler createWith(NormalizerFormat<byte[]> format) {
        return createWith(format.pipelineWith(sourceFacet.enrichRules, sourceFacet.rootPath));
    }
}
