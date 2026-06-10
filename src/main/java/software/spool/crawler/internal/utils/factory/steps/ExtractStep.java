package software.spool.crawler.internal.utils.factory.steps;

import software.spool.core.pipeline.Step;
import software.spool.core.port.serde.PayloadExtractor;

public final class ExtractStep<P, E> implements Step<P, PayloadContext<P, E>> {
    private final PayloadExtractor<P, E> extractor;

    public ExtractStep(PayloadExtractor<P, E> extractor) { this.extractor = extractor; }

    @Override public PayloadContext<P, E> apply(P input) {
        return new PayloadContext<>(input, extractor.extract(input));
    }
}
