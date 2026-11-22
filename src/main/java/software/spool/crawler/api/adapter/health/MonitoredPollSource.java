package software.spool.crawler.api.adapter.health;

import software.spool.core.exception.SpoolException;
import software.spool.core.port.health.PieceMonitor;
import software.spool.crawler.api.port.source.PollSource;

/**
 * Polls a source through another one and tells its {@link PieceMonitor} how each fetch went, so the source has a
 * circuit of its own and shows in the health of the crawler. Opening the source for a cycle is not a call: the fetch
 * is, so a cycle takes a single call of the circuit.
 *
 * @param <R> the raw type the source gives
 */
public final class MonitoredPollSource<R> implements PollSource<R> {
    private final PollSource<R> delegate;
    private final PieceMonitor monitor;

    public MonitoredPollSource(PollSource<R> delegate, PieceMonitor monitor) {
        this.delegate = delegate;
        this.monitor = monitor;
    }

    @Override
    public R fetch() throws SpoolException {
        return monitor.call(delegate::fetch);
    }

    @Override
    public PollSource<R> open() {
        PollSource<R> opened = delegate.open();
        return opened == delegate ? this : new MonitoredPollSource<>(opened, monitor);
    }

    @Override
    public String sourceId() {
        return delegate.sourceId();
    }

    @Override
    public void close() {
        delegate.close();
    }
}
