package software.spool.crawler.internal.utils.factory.steps;

import software.spool.core.pipeline.Step;
import software.spool.core.port.serde.PayloadLocator;

public final class LocateStep<P> implements Step<P, P> {
    private final PayloadLocator<P> locator;
    public LocateStep(PayloadLocator<P> locator) { this.locator = locator; }
    @Override public P apply(P input) { return locator.locate(input); }
}