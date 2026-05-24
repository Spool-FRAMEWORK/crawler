package software.spool.crawler.api.builder;

import software.spool.core.model.vo.MediaType;
import software.spool.core.port.serde.EnrichmentRule;
import software.spool.crawler.api.utils.CrawlerPorts;

import java.util.List;
import java.util.Objects;

public class StreamSourceFacet<I> extends CrawlerFacet<StreamCrawlerBuilder<I>> {

    CrawlerPorts ports;
    List<EnrichmentRule> enrichRules;
    String rootPath;
    MediaType mediaType;

    StreamSourceFacet(StreamCrawlerBuilder<I> parent) {
        super(parent);
    }

    public StreamSourceFacet<I> ports(CrawlerPorts ports) {
        this.ports = ports;
        return this;
    }

    public StreamSourceFacet<I> enrichRules(List<EnrichmentRule> enrichRules) {
        this.enrichRules = enrichRules;
        return this;
    }

    public StreamSourceFacet<I> rootPath(String rootPath) {
        this.rootPath = rootPath;
        return this;
    }

    public StreamSourceFacet<I> mediaType(MediaType mediaType) {
        this.mediaType = mediaType;
        return this;
    }

    void validate() {
        Objects.requireNonNull(ports, "CrawlerPorts must be set before calling create");
    }
}