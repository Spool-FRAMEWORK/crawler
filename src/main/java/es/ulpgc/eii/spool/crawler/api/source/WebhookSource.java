package es.ulpgc.eii.spool.crawler.api.source;

import es.ulpgc.eii.spool.crawler.api.Source;

public interface WebhookSource extends Source {
    WebhookRoute bindRoute();
}
