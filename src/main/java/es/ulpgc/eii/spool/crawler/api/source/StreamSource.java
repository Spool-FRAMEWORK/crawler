package es.ulpgc.eii.spool.crawler.api.source;

import es.ulpgc.eii.spool.core.model.RawInboxEvent;
import es.ulpgc.eii.spool.crawler.api.Source;
import es.ulpgc.eii.spool.crawler.api.exception.SpoolException;

import java.util.function.Consumer;

public interface StreamSource<R> extends Source {
    void start(Consumer<RawInboxEvent> onMessage, Consumer<Exception> onError) throws SpoolException;
    void stop();
}
