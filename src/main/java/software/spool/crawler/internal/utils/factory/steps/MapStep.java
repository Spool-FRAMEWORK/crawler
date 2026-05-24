package software.spool.crawler.internal.utils.factory.steps;

import software.spool.core.pipeline.Step;
import software.spool.core.port.serde.PayloadMapper;

public final class MapStep<I, P> implements Step<I, P> {
    private final PayloadMapper<I, P> mapper;
    public MapStep(PayloadMapper<I, P> mapper) { this.mapper = mapper; }
    @Override public P apply(I input) { return mapper.map(input); }
}