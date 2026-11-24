package software.spool.crawler.api.adapter.inbox;

import org.junit.jupiter.api.Test;
import software.spool.core.adapter.memory.InMemoryProcessedKeyRepository;
import software.spool.core.exception.DuplicateEventException;
import software.spool.core.model.EnvelopeStatus;
import software.spool.core.model.vo.Envelope;
import software.spool.core.model.vo.EventMetadata;
import software.spool.core.model.vo.IdempotencyKey;
import software.spool.core.model.vo.MediaType;
import software.spool.core.model.vo.ProcessedKey;
import software.spool.crawler.api.port.InboxWriter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DeduplicatingInboxWriterTest {
    private static final IdempotencyKey KEY = IdempotencyKey.of("boe-a-1");

    private final InMemoryProcessedKeyRepository processedKeys = new InMemoryProcessedKeyRepository();
    private final List<IdempotencyKey> written = new ArrayList<>();
    private final InboxWriter inbox = new DeduplicatingInboxWriter(new InboxWriter() {
        @Override
        public IdempotencyKey receive(Envelope envelope) {
            written.add(envelope.idempotencyKey());
            return envelope.idempotencyKey();
        }

        @Override
        public Receipt receiveReplacingQuarantined(Envelope envelope) {
            written.add(envelope.idempotencyKey());
            return Receipt.stored(envelope.idempotencyKey());
        }
    }, processedKeys);

    @Test
    void receive_aKeyStillRememberedAsProcessed_isADuplicateAndIsNotWritten() {
        processedKeys.save(new ProcessedKey(KEY, Instant.now()));

        assertThatThrownBy(() -> inbox.receive(envelope())).isInstanceOf(DuplicateEventException.class);
        assertThatThrownBy(() -> inbox.receiveReplacingQuarantined(envelope())).isInstanceOf(DuplicateEventException.class);
        assertThat(written).isEmpty();
    }

    @Test
    void receive_aKeyNotRemembered_isWrittenByTheInbox() {
        assertThat(inbox.receive(envelope())).isEqualTo(KEY);
        assertThat(inbox.receiveReplacingQuarantined(envelope())).isEqualTo(InboxWriter.Receipt.stored(KEY));
        assertThat(written).containsExactly(KEY, KEY);
    }

    private static Envelope envelope() {
        return new Envelope(KEY, new EventMetadata(), MediaType.of("application/json"), "{}".getBytes(),
                EnvelopeStatus.CAPTURED, 0, Instant.now(), null);
    }
}
