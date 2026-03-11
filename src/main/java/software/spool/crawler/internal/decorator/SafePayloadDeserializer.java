package software.spool.crawler.internal.decorator;

import software.spool.core.exception.DeserializationException;
import software.spool.core.exception.SpoolException;
import software.spool.core.port.PayloadDeserializer;

/**
 * Decorator for {@link PayloadDeserializer} that normalises unchecked exceptions
 * into typed {@link DeserializationException} instances.
 *
 * <p>
 * If the delegate's {@link #deserialize(Object)} method throws a
 * {@link SpoolException} subclass, it is re-thrown as-is. Any other
 * {@link Exception} is wrapped in a new {@link DeserializationException}.
 * </p>
 *
 * @param <R> the raw type to deserialize from
 * @param <T> the intermediate type produced after deserialization
 */
public class SafePayloadDeserializer<R, T> implements PayloadDeserializer<R, T> {
    private final PayloadDeserializer<R, T> deserializer;

    private SafePayloadDeserializer(PayloadDeserializer<R, T> deserializer) {
        this.deserializer = deserializer;
    }

    /**
     * Creates a new {@code SafePayloadDeserializer} wrapping the given delegate.
     *
     * @param <R>          the raw input type
     * @param <T>          the intermediate output type
     * @param deserializer the deserializer to wrap; must not be {@code null}
     * @return a new {@code SafePayloadDeserializer} instance
     */
    public static <R, T> SafePayloadDeserializer<R, T> of(PayloadDeserializer<R, T> deserializer) {
        return new SafePayloadDeserializer<>(deserializer);
    }

    @Override
    public T deserialize(R payload) throws DeserializationException {
        try {
            return deserializer.deserialize(payload);
        } catch (SpoolException e) {
            throw e;
        } catch (Exception e) {
            throw new DeserializationException(payload.toString(), e);
        }
    }
}
