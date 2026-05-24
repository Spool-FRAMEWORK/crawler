package software.spool.crawler.internal.utils.factory.steps;

import software.spool.core.pipeline.Step;
import software.spool.core.port.serde.PayloadDeserializer;

public final class DeserializeStep<P> implements Step<byte[], P> {
    private final PayloadDeserializer<P> deserializer;
    public DeserializeStep(PayloadDeserializer<P> deserializer) { this.deserializer = deserializer; }
    @Override public P apply(byte[] input) { return deserializer.deserialize(input); }
}