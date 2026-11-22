package software.spool.crawler.internal.control;

import java.time.Instant;

/**
 * What happened to one captured payload: stored in the inbox, already there, not stored because something failed, or
 * not stored because the circuit of the inbox is open until a given time.
 */
public sealed interface CaptureOutcome {
    CaptureOutcome STORED = new Settled("stored");
    CaptureOutcome DUPLICATE = new Settled("duplicate");
    CaptureOutcome FAILED = new Settled("failed");

    static CaptureOutcome waitingUntil(Instant until) {
        return new Waiting(until);
    }

    record Settled(String name) implements CaptureOutcome {
    }

    record Waiting(Instant until) implements CaptureOutcome {
    }
}
