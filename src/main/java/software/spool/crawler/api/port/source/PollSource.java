package software.spool.crawler.api.port.source;

import software.spool.core.exception.SpoolException;
import software.spool.crawler.api.utils.NormalizerFormat;

/**
 * A data source that is polled on demand to fetch the next payload.
 *
 * <p>
 * In every polling cycle the crawler calls {@link #open()}, then {@link #fetch()} to retrieve the raw data, and
 * then {@link #close()}. Results are then fed into the
 * configured {@link NormalizerFormat} pipeline for
 * deserialization, splitting, and serialization.
 * </p>
 *
 * <p>
 * Implementations are responsible for managing their own connection lifecycle
 * inside {@link #open()} and {@link #close()} (inherited from
 * {@link java.lang.AutoCloseable} via {@link Source}).
 * </p>
 *
 * @param <R> the raw type returned by {@link #fetch()}
 */
public interface PollSource<R> extends Source<R> {
    /**
     * Fetches the next payload from the source.
     *
     * <p>
     * This method is called by the crawler during each execution cycle.
     * Implementors should return the current snapshot of data from their source
     * (e.g. fetch a REST endpoint, query a database, read a file, etc.).
     * </p>
     *
     * @return the raw payload; must not be {@code null}
     * @throws SpoolException if the payload could not be retrieved
     */
    R fetch() throws SpoolException;

    /**
     * Opens the source for one polling cycle and returns itself.
     *
     * <p>
     * Called at the start of every cycle, and by the crawler's health check, which closes the source right after.
     * Override to set up a connection for the cycle. The default implementation is a no-op that simply returns
     * {@code this}.
     * </p>
     *
     * @return this source instance, ready to be polled
     */
    default PollSource<R> open() {
        return this;
    }
}
