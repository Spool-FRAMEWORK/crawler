package software.spool.crawler.internal.decorator;

import software.spool.core.exception.SourceSplitException;
import software.spool.core.exception.SpoolException;
import software.spool.crawler.internal.port.SourceSplitter;

import java.util.stream.Stream;

public class SafeSourceSplitter<I, O> implements SourceSplitter<I, O> {
    private final SourceSplitter<I, O> splitter;

    private SafeSourceSplitter(SourceSplitter<I, O> splitter) {
        this.splitter = splitter;
    }

    public static <I, O> SafeSourceSplitter<I, O> of(SourceSplitter<I, O> splitter) {
        return new SafeSourceSplitter<>(splitter);
    }

    @Override
    public Stream<O> split(I payload, String sourceId) throws SpoolException {
        try {
            return splitter.split(payload, sourceId);
        } catch (SpoolException e) { throw e; } catch (Exception e) {
            throw new SourceSplitException(e.getMessage(), payload.toString());
        }
    }
}
