package software.spool.crawler.api.builder;

import software.spool.core.model.vo.IdempotencyKeySchema;
import software.spool.core.model.vo.MediaType;
import software.spool.core.port.serde.EnrichmentRule;
import software.spool.core.utils.polling.PollingConfiguration;
import software.spool.crawler.api.utils.CrawlerPorts;

import java.time.Duration;
import java.util.List;
import java.util.Objects;

public class SourceFacet<I> extends CrawlerFacet<PollingCrawlerBuilder<I>> {

    CrawlerPorts ports;
    List<EnrichmentRule> enrichRules;
    String rootPath;
    PollingConfiguration schedule;
    MediaType mediaType;
    IdempotencyKeySchema idempotencyKeySchema = IdempotencyKeySchema.wholeRecord();

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

    public SourceFacet<I> mediaType(MediaType mediaType) {
        this.mediaType = mediaType;
        return this;
    }

    /**
     * Keys each record by the fields of the schema instead of by the whole record, so a field that changes on every
     * capture does not make the same record look new.
     */
    public SourceFacet<I> identifyingRecordsWith(IdempotencyKeySchema idempotencyKeySchema) {
        this.idempotencyKeySchema = Objects.requireNonNull(idempotencyKeySchema, "idempotencyKeySchema is required");
        return this;
    }

    void validate() {
        Objects.requireNonNull(ports, "CrawlerPorts must be set before calling create");
    }
}