package software.spool.crawler.internal.control.pipeline;

import java.util.function.Function;

public class Pipeline<I, O> {

    private final Function<I, Result<O>> chain;

    private Pipeline(Function<I, Result<O>> chain) {
        this.chain = chain;
    }

    public static <T> Pipeline<T, T> start() {
        return new Pipeline<>(input -> Result.ok(input));
    }

    public <K> Pipeline<I, K> add(Step<O, K> step) {
        return new Pipeline<>(input -> {
            Result<O> prev = chain.apply(input);
            if (prev instanceof Result.Error<O> err) return Result.error(err.error());
            try {
                O value = ((Result.Ok<O>) prev).value();
                return Result.ok(step.apply(value));
            } catch (Exception e) {
                return Result.error(e);
            }
        });
    }

    public Result<O> execute(I input) {
        return chain.apply(input);
    }
}