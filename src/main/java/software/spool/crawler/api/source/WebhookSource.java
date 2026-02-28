package software.spool.crawler.api.source;

import software.spool.crawler.internal.port.Source;

public interface WebhookSource extends Source {
    WebhookRoute bindRoute();
}
