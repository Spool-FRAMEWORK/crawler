package software.spool.crawler.dsl.descriptors.module.crawler;

import software.spool.crawler.dsl.descriptors.module.crawler.source.SourceDescriptor;

import java.lang.reflect.RecordComponent;
import java.util.Arrays;

public record CrawlerDescriptor(
        String id,
        SourceDescriptor source,
        EventMappingDescriptor eventMapping
) {
    public CrawlerType type() {
        String fieldName = Arrays.stream(getClass().getRecordComponents())
                .filter(component -> valueOf(component) != null)
                .map(RecordComponent::getName)
                .findFirst()
                .orElseThrow();

        return CrawlerType.valueOf(fieldName);
    }

    private Object valueOf(RecordComponent component) {
        try {
            return component.getAccessor().invoke(this);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Cannot inspect inbox descriptor", e);
        }
    }

}