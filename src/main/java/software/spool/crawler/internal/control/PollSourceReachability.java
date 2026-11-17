package software.spool.crawler.internal.control;

import software.spool.core.port.health.PolledHealthProbe;
import software.spool.crawler.api.port.source.PollSource;

/**
 * Checks that a poll source can be opened, and closes it again so the check leaves nothing open.
 */
public final class PollSourceReachability implements PolledHealthProbe.CheckedRunnable {
    private final PollSource<?> source;

    public PollSourceReachability(PollSource<?> source) {
        this.source = source;
    }

    @Override
    public void run() {
        source.open().close();
    }
}
