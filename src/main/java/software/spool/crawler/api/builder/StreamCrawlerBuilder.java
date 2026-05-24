package software.spool.crawler.api.builder;

import software.spool.core.port.serde.NamingConvention;
import software.spool.core.port.watchdog.ModuleHeartBeat;
import software.spool.crawler.api.Crawler;
import software.spool.crawler.api.port.source.StreamSource;
import software.spool.crawler.api.utils.NormalizerFormat;
import software.spool.crawler.internal.utils.factory.Normalizer;

public class StreamCrawlerBuilder<I> {

    final StreamSource<I> source;
    final ModuleHeartBeat heartBeat;
    final StreamSourceFacet<I> sourceFacet;
    final MappingFacet<StreamCrawlerBuilder<I>> mappingFacet;
    final ObservabilityFacet<StreamCrawlerBuilder<I>> observabilityFacet;

    public StreamCrawlerBuilder(StreamSource<I> source, ModuleHeartBeat heartBeat) {
        this.source = source;
        this.heartBeat = heartBeat;
        this.sourceFacet = new StreamSourceFacet<>(this);
        this.mappingFacet = new MappingFacet<>(this, new EventMappingSpecification(NamingConvention.SNAKE_CASE));
        this.observabilityFacet = new ObservabilityFacet<>(this);
    }

    public StreamSourceFacet<I> source() {
        return sourceFacet;
    }

    public MappingFacet<StreamCrawlerBuilder<I>> mapping() {
        return mappingFacet;
    }

    public ObservabilityFacet<StreamCrawlerBuilder<I>> observability() {
        return observabilityFacet;
    }

    public Crawler createWith(Normalizer<I> normalizer) {
        return new StreamCrawlerAssembler<>(this).assemble(normalizer);
    }

    public Crawler createWith(NormalizerFormat<I> format) {
        return createWith(format.pipelineWith(sourceFacet.enrichRules, sourceFacet.rootPath));
    }
}