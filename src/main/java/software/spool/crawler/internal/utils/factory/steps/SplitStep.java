package software.spool.crawler.internal.utils.factory.steps;

import software.spool.core.pipeline.Step;
import software.spool.crawler.api.port.PayloadSplitter;

import java.util.stream.Stream;

public final class SplitStep<P, R> implements Step<P, Stream<R>> {
    private final PayloadSplitter<P, R> splitter;

    public SplitStep(PayloadSplitter<P, R> splitter) { this.splitter = splitter; }

    @Override public Stream<R> apply(P input) { return splitter.split(input); }
}
