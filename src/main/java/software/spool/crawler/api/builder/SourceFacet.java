package software.spool.crawler.api.builder;

import software.spool.core.model.vo.MediaType;
import software.spool.core.port.serde.EnrichmentRule;
import software.spool.core.utils.polling.PollingConfiguration;
import software.spool.crawler.api.utils.CrawlerPorts;

import java.time.Duration;
import java.util.List;
import java.util.Objects;

public class SourceFacet<I> extends CrawlerFacet<I> {
    CrawlerPorts ports;
    List<EnrichmentRule> enrichRules;
    String rootPath;
    PollingConfiguration schedule;
    MediaType mediaType;

    SourceFacet(PollingCrawlerBuilder<I> parent) {
        super(parent);
        this.schedule = PollingConfiguration.every(Duration.ofSeconds(30));
    }

    public SourceFacet<I> ports(CrawlerPorts ports) {
        this.ports = ports;
        return this;
    }

    public SourceFacet<I> schedule(PollingConfiguration config) {
        this.schedule = config;
        return this;
    }

    public SourceFacet<I> enrichRules(List<EnrichmentRule> enrichRules) {
        this.enrichRules = enrichRules;
        return this;
    }

    public SourceFacet<I> rootPath(String rootPath) {
        this.rootPath = rootPath;
        return this;
    }

    void validate() {
        Objects.requireNonNull(ports, "CrawlerPorts must be set before calling create()");
    }

    public SourceFacet<I> mediaType(MediaType mediaType) {
        this.mediaType = mediaType;
        return this;
    }
}