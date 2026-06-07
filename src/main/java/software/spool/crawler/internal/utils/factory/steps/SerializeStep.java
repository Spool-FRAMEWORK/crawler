package software.spool.crawler.internal.utils.factory.steps;

import software.spool.core.pipeline.Step;
import software.spool.core.port.serde.RecordSerializer;

import java.util.stream.Stream;

public final class SerializeStep<R> implements Step<Stream<R>, Stream<byte[]>> {
    private final RecordSerializer<R> serializer;
    public SerializeStep(RecordSerializer<R> serializer) { this.serializer = serializer; }
    @Override public Stream<byte[]> apply(Stream<R> input) { return input.map(serializer::serialize); }
}