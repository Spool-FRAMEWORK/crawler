package es.ulpgc.eii.spool.crawler.api.source;

import es.ulpgc.eii.spool.crawler.api.Source;
import es.ulpgc.eii.spool.crawler.api.exception.SpoolException;

public interface PullSource<R> extends Source {
    R poll() throws SpoolException;
    default PullSource<R> open()  { return this; }
    String sourceId();
}
