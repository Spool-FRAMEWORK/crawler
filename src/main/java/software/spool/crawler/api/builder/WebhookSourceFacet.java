package software.spool.crawler.api.builder;

import software.spool.core.model.vo.IdempotencyKeySchema;
import software.spool.core.model.vo.MediaType;
import software.spool.core.port.authentication.RequestAuthenticator;
import software.spool.crawler.api.utils.CrawlerPorts;
import software.spool.core.port.serde.EnrichmentRule;

import java.util.List;
import java.util.Objects;

/**
 * Where a webhook crawler receives its deliveries and who may send them: the path on the node's HTTP server, the
 * authenticator that identifies the sender, the role the sender needs and the largest body it accepts.
 */
public class WebhookSourceFacet extends CrawlerFacet<WebhookCrawlerBuilder> {
    static final int DEFAULT_MAX_BODY_BYTES = 1024 * 1024;

    CrawlerPorts ports;
    List<EnrichmentRule> enrichRules = List.of();
    String rootPath;
    MediaType mediaType;
    IdempotencyKeySchema idempotencyKeySchema = IdempotencyKeySchema.wholeRecord();
    String path;
    RequestAuthenticator authenticator;
    String role;
    int maxBodyBytes = DEFAULT_MAX_BODY_BYTES;

    WebhookSourceFacet(WebhookCrawlerBuilder parent) {
        super(parent);
    }

    public WebhookSourceFacet ports(CrawlerPorts ports) {
        this.ports = ports;
        return this;
    }

    public WebhookSourceFacet enrichRules(List<EnrichmentRule> enrichRules) {
        this.enrichRules = enrichRules;
        return this;
    }

    public WebhookSourceFacet rootPath(String rootPath) {
        this.rootPath = rootPath;
        return this;
    }

    public WebhookSourceFacet mediaType(MediaType mediaType) {
        this.mediaType = mediaType;
        return this;
    }

    public WebhookSourceFacet receivingAt(String path) {
        this.path = path;
        return this;
    }

    /**
     * Every delivery is identified by the authenticator, and the sender needs the role (ADR-013).
     */
    public WebhookSourceFacet authenticatedBy(RequestAuthenticator authenticator, String role) {
        this.authenticator = authenticator;
        this.role = role;
        return this;
    }

    /**
     * Refuses bodies bigger than {@code bytes} before reading them whole. One MiB by default.
     */
    public WebhookSourceFacet acceptingAtMost(int bytes) {
        this.maxBodyBytes = bytes;
        return this;
    }

    /**
     * Keys each record by the fields of the schema instead of by the whole record, so a field that changes on every
     * capture does not make the same record look new.
     */
    public WebhookSourceFacet identifyingRecordsWith(IdempotencyKeySchema idempotencyKeySchema) {
        this.idempotencyKeySchema = Objects.requireNonNull(idempotencyKeySchema, "idempotencyKeySchema is required");
        return this;
    }

    void validate() {
        Objects.requireNonNull(ports, "CrawlerPorts must be set before calling create");
        Objects.requireNonNull(mediaType, "The media type of the deliveries must be set before calling create");
        Objects.requireNonNull(path, "The path of the webhook must be set with receivingAt(...) before calling create");
        Objects.requireNonNull(authenticator, "The webhook needs an authenticator, set with authenticatedBy(...)");
        Objects.requireNonNull(role, "The webhook needs the role its senders must have, set with authenticatedBy(...)");
    }
}
