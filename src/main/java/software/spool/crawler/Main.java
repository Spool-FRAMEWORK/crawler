package software.spool.crawler;

import software.spool.core.adapter.memory.InMemoryEventBus;
import software.spool.core.adapter.otel.OTELConfig;
import software.spool.core.model.event.SourcePayloadCaptured;
import software.spool.core.model.spool.SpoolNode;
import software.spool.core.model.vo.IdempotencyKey;
import software.spool.core.model.vo.MediaType;
import software.spool.core.port.serde.NamingConvention;
import software.spool.core.utils.media.MediaTypes;
import software.spool.core.utils.polling.PollingConfiguration;
import software.spool.crawler.api.Crawler;
import software.spool.crawler.api.builder.CrawlerBuilderFactory;
import software.spool.crawler.api.utils.CrawlerErrorRouter;
import software.spool.crawler.api.utils.CrawlerPorts;
import software.spool.crawler.api.utils.StandardNormalizer;
import software.spool.crawler.internal.adapter.http.HTTPPollSource;

import java.time.Duration;
import java.util.List;

public class Main {
    public static void main(String[] args) throws Exception {
        OTELConfig.init("crawler");
        InMemoryEventBus broker = new InMemoryEventBus();
        broker.subscribe(SourcePayloadCaptured.class, System.out::println);

        Crawler with = CrawlerBuilderFactory.poll(new HTTPPollSource("http://plytrox.com:8000/events?limit=10", "test"))
                .source()
                    .schedule(PollingConfiguration.every(Duration.ofSeconds(60)))
                    .ports(CrawlerPorts.builder()
                            .inbox(e -> IdempotencyKey.of("test", e.payload()))
                            .bus(broker).build())
                    .mediaType(MediaTypes.PDF)
                    .enrichRules(List.of())
                    .and()
                .mapping()
                    .convention(NamingConvention.SNAKE_CASE)
                    .and()
                .observability()
                    .withErrorRouter(CrawlerErrorRouter.defaults(broker))
                    .and()
                .createWith(StandardNormalizer.JSON_OBJECT);
        SpoolNode.create().register(with).start();
    }
}