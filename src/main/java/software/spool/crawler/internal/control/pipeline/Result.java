package software.spool.crawler.internal.control.pipeline;

import java.util.function.Consumer;

public sealed interface Result<T> permits Result.Ok, Result.Error {
    record Ok<T>(T value) implements Result<T> {}
    record Error<T>(Exception error) implements Result<T> {}

    static <T> Result<T> ok(T value) {
        return new Ok<>(value);
    }

    static <T> Result<T> error(Exception e) {
        return new Error<>(e);
    }

    default Result<T> peek(Consumer<T> consumer) {
        if (this instanceof Ok<T> ok) {
            consumer.accept(ok.value());
        }
        return this;
    }

    default Result<T> peekError(Consumer<Exception> consumer) {
        if (this instanceof Error<T> err) {
            consumer.accept(err.error());
        }
        return this;
    }
}