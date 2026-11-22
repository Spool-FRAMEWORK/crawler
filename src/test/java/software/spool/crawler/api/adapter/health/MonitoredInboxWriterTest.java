package software.spool.crawler.api.adapter.health;

import org.junit.jupiter.api.Test;
import software.spool.core.exception.DuplicateEventException;
import software.spool.core.exception.InboxWriteException;
import software.spool.core.model.vo.IdempotencyKey;
import software.spool.core.port.health.HealthStatus;
import software.spool.core.port.health.PieceCheckPolicy;
import software.spool.core.port.health.PieceMonitor;
import software.spool.crawler.api.port.InboxWriter;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MonitoredInboxWriterTest {
    private static final IdempotencyKey KEY = IdempotencyKey.of("payment-1");

    private final PieceMonitor monitor = PieceMonitor.of("inbox", "S3", Optional.empty(), PieceCheckPolicy.defaults());

    @Test
    void receive_whenTheInboxFails_marksThePieceUnhealthy() {
        InboxWriter writer = new MonitoredInboxWriter(envelope -> {
            throw new InboxWriteException("the bucket refused the object");
        }, monitor);

        assertThatThrownBy(() -> writer.receive(null)).isInstanceOf(InboxWriteException.class);
        assertThat(monitor.health().status()).isEqualTo(HealthStatus.UNHEALTHY);
    }

    @Test
    void receive_whenTheInboxAlreadyHasTheEnvelope_leavesThePieceHealthy() {
        InboxWriter writer = new MonitoredInboxWriter(envelope -> {
            throw new DuplicateEventException(KEY);
        }, monitor);

        assertThatThrownBy(() -> writer.receive(null)).isInstanceOf(DuplicateEventException.class);
        assertThat(monitor.health().status()).isEqualTo(HealthStatus.HEALTHY);
    }

    @Test
    void receive_givesTheKeyTheInboxKept() {
        InboxWriter writer = new MonitoredInboxWriter(envelope -> KEY, monitor);

        assertThat(writer.receive(null)).isEqualTo(KEY);
    }
}
