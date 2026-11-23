package software.spool.crawler.api.port;

import software.spool.core.exception.*;
import software.spool.core.model.vo.IdempotencyKey;
import software.spool.core.model.vo.Envelope;

public interface InboxWriter {
    /**
     * Stores a serialized payload in the inbox, using the provided idempotency key
     * to prevent duplicate entries.
     *
     * @return the identifier assigned to the stored entry
     * @throws InboxWriteException if the entry could not be persisted
     * @throws DuplicateEventException if the entry was duplicated
     */
    IdempotencyKey receive(Envelope envelope) throws InboxWriteException, DuplicateEventException;

    /**
     * Stores the envelope as {@link #receive} does, except that when its key is in quarantine with another payload,
     * the new envelope takes the place of the one in quarantine (ADR-032). The same payload is still a duplicate. An
     * inbox that cannot replace stores the envelope as {@link #receive} does.
     *
     * @throws InboxWriteException if the entry could not be persisted
     * @throws DuplicateEventException if the key is already there and cannot be replaced
     */
    default Receipt receiveReplacingQuarantined(Envelope envelope) throws InboxWriteException, DuplicateEventException {
        return Receipt.stored(receive(envelope));
    }

    /** What the inbox did with an envelope: its key, and whether it took the place of one in quarantine. */
    record Receipt(IdempotencyKey key, boolean replacedQuarantined) {
        public static Receipt stored(IdempotencyKey key) {
            return new Receipt(key, false);
        }

        public static Receipt replacedQuarantined(IdempotencyKey key) {
            return new Receipt(key, true);
        }
    }
}
