package software.spool.crawler.api.builder;

import software.spool.core.model.Event;
import software.spool.core.model.vo.IdempotencyKey;
import software.spool.core.port.serde.NamingConvention;

import java.util.List;
import java.util.function.BiFunction;

public class MappingFacet<B> extends CrawlerFacet<B> {

    private EventMappingSpecification spec;

    MappingFacet(B parent, EventMappingSpecification spec) {
        super(parent);
        this.spec = spec;
    }

    public MappingFacet<B> convention(NamingConvention convention) {
        this.spec = new EventMappingSpecification(convention);
        return this;
    }

    public MappingFacet<B> addDomainEvent(Class<? extends Event> eventType, String... partitionAttributes) {
        spec.addDomainEvent(eventType, partitionAttributes);
        return this;
    }

    public MappingFacet<B> addDomainEvent(List<Class<? extends Event>> eventTypeList, String... partitionAttributes) {
        spec.addDomainEvent(eventTypeList, partitionAttributes);
        return this;
    }

    public <D> MappingFacet<B> addDomainEvent(Class<D> dtoType, BiFunction<D, IdempotencyKey, Event> toEvent, String... partitionAttributes) {
        spec.addDomainEvent(dtoType, toEvent, partitionAttributes);
        return this;
    }

    public MappingFacet<B> addPartitionAttributes(String... attributes) {
        spec.addPartitionAttributes(attributes);
        return this;
    }

    EventMappingSpecification spec() {
        return spec;
    }
}