package software.spool.crawler.api.adapter.health;

import software.spool.core.exception.DuplicateEventException;
import software.spool.core.exception.InboxWriteException;
import software.spool.core.model.vo.Envelope;
import software.spool.core.model.vo.IdempotencyKey;
import software.spool.core.port.health.PieceMonitor;
import software.spool.crawler.api.port.InboxWriter;

import java.util.List;

/**
 * Writes to the inbox through another writer and tells its {@link PieceMonitor} how each write went. An envelope
 * the inbox already has is an answer of the inbox, not a failure of it.
 */
public final class MonitoredInboxWriter implements InboxWriter {
    private static final List<Class<? extends Exception>> ANSWERS = List.of(DuplicateEventException.class);

    private final InboxWriter delegate;
    private final PieceMonitor monitor;

    public MonitoredInboxWriter(InboxWriter delegate, PieceMonitor monitor) {
        this.delegate = delegate;
        this.monitor = monitor;
    }

    @Override
    public IdempotencyKey receive(Envelope envelope) throws InboxWriteException, DuplicateEventException {
        return monitor.call(() -> delegate.receive(envelope), ANSWERS);
    }
}
