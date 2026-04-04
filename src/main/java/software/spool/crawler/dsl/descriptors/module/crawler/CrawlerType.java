package software.spool.crawler.dsl.descriptors.module.crawler;

import java.util.Arrays;

public enum CrawlerType {
    POLL("poll"),
    STREAM("stream"),
    WEBHOOK("webhook");

    private final String fieldName;

    CrawlerType(String fieldName) {
        this.fieldName = fieldName;
    }

    public static CrawlerType fromFieldName(String fieldName) {
        return Arrays.stream(values())
                .filter(type -> type.fieldName.equals(fieldName))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown field: " + fieldName));
    }
}
