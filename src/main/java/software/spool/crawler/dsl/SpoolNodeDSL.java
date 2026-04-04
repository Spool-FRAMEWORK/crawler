package software.spool.crawler.dsl;

import software.spool.core.adapter.jackson.PayloadDeserializerFactory;
import software.spool.core.model.spool.SpoolModule;
import software.spool.core.model.spool.SpoolNode;
import software.spool.core.port.serde.NamingConvention;
import software.spool.core.utils.polling.PollingConfiguration;
import software.spool.crawler.Main;
import software.spool.crawler.api.builder.CrawlerBuilderFactory;
import software.spool.crawler.api.builder.EventMappingSpecification;
import software.spool.crawler.api.port.source.PollSource;
import software.spool.crawler.api.utils.*;
import software.spool.crawler.dsl.descriptors.SpoolNodeDescriptor;
import software.spool.crawler.dsl.descriptors.infrastructure.*;
import software.spool.crawler.dsl.descriptors.module.SpoolModuleDescriptor;
import software.spool.crawler.dsl.descriptors.module.crawler.*;
import software.spool.crawler.dsl.descriptors.module.crawler.source.poll.ScheduleDescriptor;
import software.spool.crawler.dsl.descriptors.module.crawler.source.SourceDescriptor;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.Objects;

public abstract class SpoolNodeDSL {
    public static void fromDescriptor(String path) throws IOException {
        try(BufferedInputStream is = new BufferedInputStream(
                Objects.requireNonNull(Main.class.getResourceAsStream(path)))) {
            fromDescriptor(PayloadDeserializerFactory.yaml().as(SpoolNodeDescriptor.class)
                    .deserialize(new String(is.readAllBytes())));
        } catch (Exception e) {
            throw new IOException(e);
        }
    }

    public static void fromDescriptor(SpoolNodeDescriptor descriptor) throws IOException {
        SpoolNode node = SpoolNode.create();
        buildModulesFrom(descriptor.spoolModuleList(), descriptor.infrastructure())
                .forEach(node::register);
        node.start();
    }

    private static List<SpoolModule> buildModulesFrom(
            List<SpoolModuleDescriptor> moduleDescriptors,
            InfrastructureDescriptor infrastructure
    ) {
        return moduleDescriptors.stream()
                .map(m -> buildModuleFrom(m, infrastructure))
                .toList();
    }

    private static SpoolModule buildModuleFrom(
            SpoolModuleDescriptor moduleDescriptor,
            InfrastructureDescriptor infrastructure
    ) {
        return buildCrawlerFrom(moduleDescriptor.crawler(), infrastructure);
    }

    private static SpoolModule buildCrawlerFrom(
            CrawlerDescriptor crawlerDescriptor,
            InfrastructureDescriptor infrastructure
    ) {
        return CrawlerBuilderFactory.watchdog(infrastructure.watchdog().url(), crawlerDescriptor.id())
                .poll(buildPollSourceFrom(crawlerDescriptor.source()))
                .schedule(buildScheduleFrom(crawlerDescriptor.source().poll().schedule()))
                .ports(buildPortsFrom(infrastructure))
                .eventMapping(buildEventMappingFrom(crawlerDescriptor.eventMapping()))
                .createWith(StandardFormat.valueOf(crawlerDescriptor.source().format()));
    }

    private static PollSource<?> buildPollSourceFrom(SourceDescriptor sourceDescriptor) {
        return SourceFactory.pollFrom(sourceDescriptor);
    }

    private static PollingConfiguration buildScheduleFrom(ScheduleDescriptor scheduleDescriptor) {
        return PollingConfiguration.every(Duration.ofSeconds(scheduleDescriptor.every()));
    }

    private static EventMappingSpecification buildEventMappingFrom(EventMappingDescriptor eventMappingDescriptor) {
        return new EventMappingSpecification(NamingConvention.SNAKE_CASE)
                .addPartitionAttributes(eventMappingDescriptor.attributeList().stream()
                        .map(PartitionAttributeDescriptor::value)
                        .toArray(String[]::new));
    }

    private static CrawlerPorts buildPortsFrom(InfrastructureDescriptor infrastructure) {
        return CrawlerPorts.builder()
                .bus(EventBusFactory.from(infrastructure.eventBus()))
                .inbox(InboxWriterFactory.from(infrastructure.inbox()))
                .build();
    }
}
