package software.spool.crawler.api.utils;

import org.junit.jupiter.api.Test;
import software.spool.core.exception.DuplicateEventException;
import software.spool.core.model.vo.IdempotencyKey;
import software.spool.core.port.logging.Logger;
import software.spool.core.utils.routing.ErrorRouter;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CrawlerErrorRouterTest {

    private final RecordingLogger log = new RecordingLogger();
    private final ErrorRouter router = CrawlerErrorRouter.defaults(null, log);

    @Test
    void aDuplicateEventLeavesNoErrorInTheLog() {
        router.dispatch(new DuplicateEventException(IdempotencyKey.of("source", "payload".getBytes())));

        assertThat(log.errors).isEmpty();
    }

    @Test
    void aDuplicateEventIsRejectedWithoutThrowing() {
        router.dispatch(new DuplicateEventException(IdempotencyKey.of("source", "payload".getBytes())));

        assertThat(log.lines).hasSize(1).first().asString().startsWith("DEBUG");
    }

    @Test
    void aRealErrorIsStillLoggedAsAnError() {
        router.dispatch(new IllegalStateException("the inbox is down"));

        assertThat(log.errors).containsExactly("the inbox is down");
    }

    @Test
    void aSubclassOfTheDuplicateEventIsAlsoRejectedQuietly() {
        router.dispatch(new DuplicateEventException(IdempotencyKey.of("source", "payload".getBytes())) {});

        assertThat(log.errors).isEmpty();
    }

    /** Keeps what was logged so a test can tell the levels apart. */
    private static final class RecordingLogger implements Logger {
        final List<String> lines = new ArrayList<>();
        final List<String> errors = new ArrayList<>();

        private void add(String level, String message) {
            lines.add(level + " " + message);
            if (level.equals("ERROR")) errors.add(message);
        }

        @Override public void debug(String message) { add("DEBUG", message); }
        @Override public void debug(String format, Object... args) { add("DEBUG", format); }
        @Override public void info(String message) { add("INFO", message); }
        @Override public void info(String format, Object... args) { add("INFO", format); }
        @Override public void warn(String message) { add("WARN", message); }
        @Override public void warn(String format, Object... args) { add("WARN", format); }
        @Override public void warn(String message, Throwable cause) { add("WARN", message); }
        @Override public void error(String message) { add("ERROR", message); }
        @Override public void error(String format, Object... args) { add("ERROR", format); }
        @Override public void error(String message, Throwable cause) { add("ERROR", message); }
    }
}
