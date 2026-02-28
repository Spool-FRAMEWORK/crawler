package software.spool.crawler.internal.port;

public interface Source extends AutoCloseable {
    String sourceId();
    default void close() {}
}
