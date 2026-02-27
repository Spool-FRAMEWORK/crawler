package es.ulpgc.eii.spool.crawler.api;

import es.ulpgc.eii.spool.crawler.api.exception.SpoolException;

import java.util.stream.Stream;

@FunctionalInterface
public interface SourceSplitter<I, O> {
    Stream<O> split(I payload, String sourceId) throws SpoolException;
}
