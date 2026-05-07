package software.spool.crawler.api.port.source;

import software.spool.core.exception.SpoolException;
import software.spool.crawler.api.utils.NormalizerFormat;

/**
 * A data source that is polled on demand to fetch the next payload.
 *
 * <p>
 * The crawler calls {@link #open()} once before starting the polling cycle
 * and {@link #fetch()} to retrieve the raw data. Results are then fed into the
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
     * Opens (or re-opens) the source and returns itself.
     *
     * <p>
     * Override to perform any connection setup before polling begins. The
     * default implementation is a no-op that simply returns {@code this}.
     * </p>
     *
     * @return this source instance, ready to be polled
     */
    default PollSource<R> open() {
        return this;
    }
}
