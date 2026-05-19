package software.spool.crawler.api.builder;

import software.spool.core.port.serde.NamingConvention;
import software.spool.core.port.watchdog.ModuleHeartBeat;
import software.spool.crawler.api.Crawler;
import software.spool.crawler.api.port.source.PollSource;
import software.spool.crawler.api.utils.NormalizerFormat;
import software.spool.crawler.internal.port.decorator.SafePollSource;
import software.spool.crawler.internal.utils.factory.Normalizer;

public class PollingCrawlerBuilder<I> {

    final PollSource<I> source;
    final ModuleHeartBeat heartBeat;
    final SourceFacet<I> sourceFacet;
    final MappingFacet<I> mappingFacet;
    final ObservabilityFacet<I> observabilityFacet;

    public PollingCrawlerBuilder(PollSource<I> source, ModuleHeartBeat heartBeat) {
        this.source = SafePollSource.of(source);
        this.heartBeat = heartBeat;
        this.sourceFacet = new SourceFacet<>(this);
        this.mappingFacet = new MappingFacet<>(this, new EventMappingSpecification(NamingConvention.SNAKE_CASE));
        this.observabilityFacet = new ObservabilityFacet<>(this);
    }

    public SourceFacet<I> source() {
        return sourceFacet;
    }

    public MappingFacet<I> mapping() {
        return mappingFacet;
    }

    public ObservabilityFacet<I> observability() {
        return observabilityFacet;
    }

    public <P, E, R> Crawler createWith(Normalizer<P, E, R> normalizer) {
        return new PollingCrawlerAssembler<>(this).assemble(normalizer);
    }

    public <P, E, R> Crawler createWith(NormalizerFormat<P, E, R> format) {
        return createWith(format.pipelineWith(sourceFacet.enrichRules, sourceFacet.rootPath));
    }
}