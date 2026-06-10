package software.spool.crawler.internal.port.decorator;

import org.junit.jupiter.api.Test;
import software.spool.core.exception.InboxWriteException;
import software.spool.core.model.EnvelopeStatus;
import software.spool.core.model.vo.*;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SafeInboxWriterTest {

    @Test
    void receive_successfulWrite_delegatesToInner() {
        IdempotencyKey expected = IdempotencyKey.of("result");
        SafeInboxWriter writer = SafeInboxWriter.of(env -> expected);

        IdempotencyKey result = writer.receive(anyEnvelope());

        assertThat(result).isEqualTo(expected);
    }

    @Test
    void receive_innerThrowsRuntimeException_wrapsInInboxWriteException() {
        SafeInboxWriter writer = SafeInboxWriter.of(env -> { throw new RuntimeException("db error"); });

        assertThatThrownBy(() -> writer.receive(anyEnvelope()))
            .isInstanceOf(InboxWriteException.class)
            .hasMessageContaining("db error");
    }

    private static Envelope anyEnvelope() {
        return new Envelope(
            IdempotencyKey.of("src", "{}".getBytes()),
            new EventMetadata(),
            MediaType.of("application/json"),
            "{}".getBytes(),
            EnvelopeStatus.CAPTURED,
            0,
            Instant.now(),
            null
        );
    }
}
