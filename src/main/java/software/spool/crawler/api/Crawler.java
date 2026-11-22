package software.spool.crawler.api;

import software.spool.core.port.health.HealthProvider;
import software.spool.core.port.http.HttpRoute;

import software.spool.core.model.spool.SpoolModule;
import software.spool.core.model.spool.SpoolNode;
import software.spool.core.port.health.HealthCheck;
import software.spool.core.port.health.HealthProbe;
import software.spool.core.port.health.ModuleHealthPayload;
import software.spool.core.port.watchdog.ModuleHeartBeat;
import software.spool.core.utils.polling.CancellationToken;
import software.spool.core.utils.routing.ErrorRouter;
import software.spool.crawler.api.strategy.CrawlerStrategy;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

public class Crawler implements SpoolModule {
    private final CrawlerStrategy strategy;
    private volatile CancellationToken token;
    private final ErrorRouter errorRouter;
    private final ModuleHeartBeat heartBeat;
    private final List<HealthProbe> probes;
    private final List<HttpRoute> routes;

    public Crawler(CrawlerStrategy strategy, ErrorRouter errorRouter, ModuleHeartBeat heartBeat, List<HealthProbe> probes) {
        this(strategy, errorRouter, heartBeat, probes, List.of());
    }

    /**
     * @param routes what the crawler answers on its node's HTTP server, such as the route of a webhook
     */
    public Crawler(CrawlerStrategy strategy, ErrorRouter errorRouter, ModuleHeartBeat heartBeat, List<HealthProbe> probes,
                   List<HttpRoute> routes) {
        this.routes = List.copyOf(routes);
        this.strategy = strategy;
        this.errorRouter = errorRouter;
        this.heartBeat = heartBeat;
        heartBeat.reportFrom(this);
        this.probes = probes;
        this.token = CancellationToken.NOOP;
    }

    @Override
    public void start(SpoolNode.StartPermit permit) {
        if (token.isActive()) return;
        Objects.requireNonNull(permit);
        token = CancellationToken.create();
        try {
            heartBeat.start();
            strategy.execute(token);
        } catch (Exception e) { errorRouter.dispatch(e); }
    }

    @Override
    public void stop(SpoolNode.StartPermit permit) {
        if (!token.isActive()) return;
        Objects.requireNonNull(permit);
        token.cancel();
        strategy.stop();
        heartBeat.stop();
        token = CancellationToken.NOOP;
    }

    @Override
    public Collection<HttpRoute> routes() {
        return routes;
    }

    @Override
    public void reportHealthFrom(HealthProvider health) {
        heartBeat.reportFrom(health);
    }

    @Override
    public ModuleHealthPayload checkHealth() {
        String moduleId = heartBeat.identity().moduleId();
        if (!token.isActive())
            return ModuleHealthPayload.degraded(moduleId, List.of());
        List<HealthCheck> checks = probes.stream()
                .map(HealthProbe::probe)
                .toList();
        return ModuleHealthPayload.of(moduleId, checks);
    }
}
