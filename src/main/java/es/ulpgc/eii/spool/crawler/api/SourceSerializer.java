package es.ulpgc.eii.spool.crawler.api;

import es.ulpgc.eii.spool.crawler.api.exception.SpoolException;

public interface SourceSerializer<T> {
    String wrap(T record, String sourceId) throws SpoolException;
}
