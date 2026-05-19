package software.spool.crawler.api.builder;

import software.spool.core.model.Event;
import software.spool.core.model.vo.IdempotencyKey;
import software.spool.core.port.serde.NamingConvention;

import java.util.function.BiFunction;

public class MappingFacet<I> extends CrawlerFacet<I> {

    private EventMappingSpecification spec;

    MappingFacet(PollingCrawlerBuilder<I> parent, EventMappingSpecification spec) {
        super(parent);
        this.spec = spec;
    }

    public MappingFacet<I> convention(NamingConvention convention) {
        this.spec = new EventMappingSpecification(convention);
        return this;
    }

    public MappingFacet<I> addDomainEvent(Class<? extends Event> eventType, String... partitionAttributes) {
        spec.addDomainEvent(eventType, partitionAttributes);
        return this;
    }

    public <D> MappingFacet<I> addDomainEvent(Class<D> dtoType, BiFunction<D, IdempotencyKey, Event> toEvent, String... partitionAttributes) {
        spec.addDomainEvent(dtoType, toEvent, partitionAttributes);
        return this;
    }

    public MappingFacet<I> addPartitionAttributes(String... attributes) {
        spec.addPartitionAttributes(attributes);
        return this;
    }

    EventMappingSpecification spec() {
        return spec;
    }
}