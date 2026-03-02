package software.spool.crawler.internal.decorator;

import software.spool.core.exception.SourcePollException;
import software.spool.core.exception.SpoolException;
import software.spool.crawler.api.source.PollSource;

public class SafePollSource<R> implements PollSource<R> {
    private final PollSource<R> source;

    private SafePollSource(PollSource<R> source) {
        this.source = source;
    }

    public static <R> SafePollSource<R> of(PollSource<R> source) {
        return new SafePollSource<>(source);
    }

    @Override
    public R poll() throws SpoolException {
        try {
            return source.poll();
        } catch (Exception e) {
            throw new SourcePollException("Source poll failed: " + e.getMessage(), e);
        }
    }

    @Override
    public String sourceId() {
        return source.sourceId();
    }
}
