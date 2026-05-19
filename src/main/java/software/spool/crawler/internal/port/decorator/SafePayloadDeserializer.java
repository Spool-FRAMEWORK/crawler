package software.spool.crawler.internal.port.decorator;

import software.spool.core.exception.DeserializationException;
import software.spool.core.exception.SpoolException;
import software.spool.core.port.serde.PayloadDeserializer;

public class SafePayloadDeserializer<T> implements PayloadDeserializer<T> {
    private final PayloadDeserializer<T> deserializer;

    private SafePayloadDeserializer(PayloadDeserializer<T> deserializer) {
        this.deserializer = deserializer;
    }

    public static <T> SafePayloadDeserializer<T> of(PayloadDeserializer<T> deserializer) {
        return new SafePayloadDeserializer<>(deserializer);
    }

    @Override
    public T deserialize(byte[] payload) throws DeserializationException {
        try {
            return deserializer.deserialize(payload);
        } catch (SpoolException e) {
            throw e;
        } catch (Exception e) {
            throw new DeserializationException(new String(payload), e.getMessage());
        }
    }
}
