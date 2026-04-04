package software.spool.crawler.dsl.descriptors.infrastructure;

import java.util.Objects;

public record WatchdogDescriptor(
        String url
) {
    public WatchdogDescriptor {
        Objects.requireNonNull(url);
    }
}
