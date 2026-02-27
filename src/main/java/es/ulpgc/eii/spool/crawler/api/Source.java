package es.ulpgc.eii.spool.crawler.api;

public interface Source extends AutoCloseable {
    String sourceId();
    default void close() {}
}
