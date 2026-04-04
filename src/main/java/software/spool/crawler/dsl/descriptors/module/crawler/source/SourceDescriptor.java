package software.spool.crawler.dsl.descriptors.module.crawler.source;

import software.spool.crawler.dsl.descriptors.module.crawler.CrawlerType;
import software.spool.crawler.dsl.descriptors.module.crawler.source.poll.PollSourceDescriptor;

import java.lang.reflect.RecordComponent;
import java.util.Arrays;

public record SourceDescriptor(
    String id,
    String format,
    PollSourceDescriptor poll
) {
    public CrawlerType type() {
        String fieldName = Arrays.stream(getClass().getRecordComponents())
                .filter(component -> valueOf(component) != null)
                .map(RecordComponent::getName)
                .filter(name -> !name.startsWith("id") && !name.startsWith("format"))
                .findFirst()
                .orElseThrow();
        return CrawlerType.fromFieldName(fieldName);
    }

    private Object valueOf(RecordComponent component) {
        try {
            return component.getAccessor().invoke(this);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Cannot inspect source descriptor", e);
        }
    }
}
