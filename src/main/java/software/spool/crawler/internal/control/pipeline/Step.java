package software.spool.crawler.internal.control.pipeline;

import java.util.function.Function;

@FunctionalInterface
public interface Step<I, O> extends Function<I, O> {}