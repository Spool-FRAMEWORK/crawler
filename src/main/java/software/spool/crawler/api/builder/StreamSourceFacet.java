package software.spool.crawler.api.builder;

import software.spool.crawler.api.port.WhenQuarantined;
import software.spool.core.model.vo.IdempotencyKeySchema;
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
    IdempotencyKeySchema idempotencyKeySchema = IdempotencyKeySchema.wholeRecord();
    WhenQuarantined whenQuarantined = WhenQuarantined.KEEP;

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

    /**
     * Keys each record by the fields of the schema instead of by the whole record, so a field that changes on every
     * capture does not make the same record look new.
     */
    public StreamSourceFacet<I> identifyingRecordsWith(IdempotencyKeySchema idempotencyKeySchema) {
        this.idempotencyKeySchema = Objects.requireNonNull(idempotencyKeySchema, "idempotencyKeySchema is required");
        return this;
    }

    /**
     * What to do with a capture whose key is in quarantine in the inbox: refuse it, or let one with another payload
     * take its place (ADR-032).
     */
    public StreamSourceFacet<I> whenQuarantined(WhenQuarantined whenQuarantined) {
        this.whenQuarantined = Objects.requireNonNull(whenQuarantined, "whenQuarantined is required");
        return this;
    }

    void validate() {
        Objects.requireNonNull(ports, "CrawlerPorts must be set before calling create");
    }
}