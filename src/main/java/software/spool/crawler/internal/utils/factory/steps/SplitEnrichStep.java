package software.spool.crawler.internal.utils.factory.steps;

import software.spool.core.pipeline.Step;
import software.spool.core.port.serde.PayloadExtractor;
import software.spool.core.port.serde.RecordEnricher;
import software.spool.crawler.api.port.PayloadSplitter;

import java.util.stream.Stream;

public final class SplitEnrichStep<P, R> implements Step<P, Stream<R>> {
    private final PayloadSplitter<P, R> splitter;
    private final PayloadExtractor<P, R> extractor;
    private final RecordEnricher<R, R> enricher;

    public SplitEnrichStep(PayloadSplitter<P, R> splitter, PayloadExtractor<P, R> extractor, RecordEnricher<R, R> enricher) {
        this.splitter = splitter;
        this.extractor = extractor;
        this.enricher = enricher;
    }

    @Override public Stream<R> apply(P input) {
        return enricher.enrich(splitter.split(input), extractor.extract(input));
    }
}