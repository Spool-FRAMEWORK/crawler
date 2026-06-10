package software.spool.crawler.internal.utils.factory.steps;

import software.spool.core.pipeline.Step;
import software.spool.core.port.serde.PayloadLocator;

public final class ContextLocateStep<P, E> implements Step<PayloadContext<P, E>, PayloadContext<P, E>> {
    private final PayloadLocator<P> locator;

    public ContextLocateStep(PayloadLocator<P> locator) { this.locator = locator; }

    @Override public PayloadContext<P, E> apply(PayloadContext<P, E> input) {
        return new PayloadContext<>(locator.locate(input.payload()), input.enrichment());
    }
}
