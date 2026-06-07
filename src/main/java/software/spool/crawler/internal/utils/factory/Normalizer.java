package software.spool.crawler.internal.utils.factory;

import software.spool.core.adapter.logging.LoggerFactory;
import software.spool.core.pipeline.Pipeline;
import software.spool.core.pipeline.Result;
import software.spool.core.port.logging.Logger;

import java.util.stream.Stream;

public final class Normalizer<I> {

    private static final Logger LOGGER = LoggerFactory.getLogger(Normalizer.class);

    private final Pipeline<I, Stream<byte[]>> pipeline;

    public Normalizer(Pipeline<I, Stream<byte[]>> pipeline) {
        this.pipeline = pipeline;
    }

    public Stream<byte[]> normalize(I input) {
        return switch (pipeline.execute(input)) {
            case Result.Ok<Stream<byte[]>> ok -> ok.value();
            case Result.Error<Stream<byte[]>> err -> {
                LOGGER.error("Normalization failed", err.error());
                yield Stream.empty();
            }
        };
    }
}