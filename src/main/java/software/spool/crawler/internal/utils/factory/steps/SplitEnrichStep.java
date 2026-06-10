package software.spool.crawler.internal.utils.factory.steps;

import software.spool.core.pipeline.Step;
import software.spool.core.port.serde.RecordEnricher;
import software.spool.crawler.api.port.PayloadSplitter;

import java.util.stream.Stream;

public final class SplitEnrichStep<P, E, R> implements Step<PayloadContext<P, E>, Stream<R>> {
    private final PayloadSplitter<P, R> splitter;
    private final RecordEnricher<R, E> enricher;

    public SplitEnrichStep(PayloadSplitter<P, R> splitter, RecordEnricher<R, E> enricher) {
        this.splitter = splitter;
        this.enricher = enricher;
    }

    @Override public Stream<R> apply(PayloadContext<P, E> input) {
        return enricher.enrich(splitter.split(input.payload()), input.enrichment());
    }
}
