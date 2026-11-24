package software.spool.crawler.api.adapter.inbox;

import software.spool.core.exception.DuplicateEventException;
import software.spool.core.exception.InboxWriteException;
import software.spool.core.model.vo.Envelope;
import software.spool.core.model.vo.IdempotencyKey;
import software.spool.core.port.idempotency.ProcessedKeyRepository;
import software.spool.crawler.api.port.InboxWriter;

/**
 * Writes to the inbox through another writer, but first refuses as a duplicate a key still remembered as processed:
 * the key of an envelope the janitor removed by its TTL within the deduplication window of the inbox (ADR-033).
 */
public final class DeduplicatingInboxWriter implements InboxWriter {
    private final InboxWriter delegate;
    private final ProcessedKeyRepository processedKeys;

    public DeduplicatingInboxWriter(InboxWriter delegate, ProcessedKeyRepository processedKeys) {
        this.delegate = delegate;
        this.processedKeys = processedKeys;
    }

    @Override
    public IdempotencyKey receive(Envelope envelope) throws InboxWriteException, DuplicateEventException {
        refuseIfRemembered(envelope);
        return delegate.receive(envelope);
    }

    @Override
    public Receipt receiveReplacingQuarantined(Envelope envelope) throws InboxWriteException, DuplicateEventException {
        refuseIfRemembered(envelope);
        return delegate.receiveReplacingQuarantined(envelope);
    }

    private void refuseIfRemembered(Envelope envelope) {
        if (processedKeys.contains(envelope.idempotencyKey())) throw new DuplicateEventException(envelope.idempotencyKey());
    }
}
