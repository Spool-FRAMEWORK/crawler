package software.spool.crawler.api.builder;

import software.spool.core.adapter.otel.OpenTelemetryMetricsRegistry;
import software.spool.core.port.http.HttpMethod;
import software.spool.core.port.http.HttpRoute;
import software.spool.core.port.http.RouteAccess;
import software.spool.core.port.metrics.MetricsRegistry;
import software.spool.core.port.metrics.SpoolMetrics;
import software.spool.core.utils.routing.ErrorRouter;
import software.spool.crawler.api.Crawler;
import software.spool.crawler.api.normalizer.Normalizer;
import software.spool.crawler.api.utils.CrawlerErrorRouter;
import software.spool.crawler.internal.adapter.input.http.WebhookRouteHandler;
import software.spool.crawler.internal.control.PayloadCapturedHandler;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

class WebhookCrawlerAssembler {
    private static final String PAYLOAD_SIZE = "spool.crawler.payload.size";

    private final WebhookCrawlerBuilder config;

    WebhookCrawlerAssembler(WebhookCrawlerBuilder config) {
        this.config = config;
    }

    Crawler assemble(Normalizer<byte[]> normalizer) {
        WebhookSourceFacet source = config.sourceFacet;
        source.validate();
        MetricsRegistry metrics = Objects.requireNonNullElse(config.observabilityFacet.metricsRegistry, new OpenTelemetryMetricsRegistry());
        ErrorRouter errorRouter = Objects.requireNonNullElse(config.observabilityFacet.errorRouter, CrawlerErrorRouter.defaults(source.ports.bus()));
        PayloadCapturedHandler capture = new PayloadCapturedHandler(
                CapturePipeline.of(config.mappingFacet.spec(), source.ports, source.idempotencyKeySchema, metrics.histogram(PAYLOAD_SIZE, "", "By")),
                config.sourceId, source.mediaType, errorRouter,
                metrics.counter(SpoolMetrics.Crawler.EVENTS_TOTAL, SpoolMetrics.Crawler.EVENTS_TOTAL_DESC, "events"),
                metrics.counter(SpoolMetrics.Crawler.ERRORS_TOTAL, SpoolMetrics.Crawler.ERRORS_TOTAL_DESC, "errors"),
                metrics.timer(SpoolMetrics.Crawler.LATENCY, SpoolMetrics.Crawler.LATENCY_DESC, "s"));
        HttpRoute route = new HttpRoute(HttpMethod.POST, source.path, new RouteAccess.Protected(source.authenticator, source.role),
                new WebhookRouteHandler(normalizer, source.mediaType, capture)).withMaxBodyBytes(source.maxBodyBytes);
        return new Crawler(token -> { }, errorRouter, config.heartBeat, new ArrayList<>(source.ports.healthProbes()), List.of(route));
    }
}
