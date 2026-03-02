package software.spool.crawler.internal.decorator;

import software.spool.core.exception.DeserializationException;
import software.spool.core.exception.SpoolException;
import software.spool.crawler.internal.port.SourceDeserializer;

public class SafeSourceDeserializer<R, T> implements SourceDeserializer<R, T> {
    private final SourceDeserializer<R, T> deserializer;

    private SafeSourceDeserializer(SourceDeserializer<R, T> deserializer) {
        this.deserializer = deserializer;
    }

    public static <R, T> SafeSourceDeserializer<R, T> of(SourceDeserializer<R, T> deserializer) {
        return new SafeSourceDeserializer<>(deserializer);
    }

    @Override
    public T deserialize(R source) throws DeserializationException {
        try {
            return deserializer.deserialize(source);
        } catch (SpoolException e) { throw e; } catch (Exception e) {
            throw new DeserializationException("Error when deserializing: " + e.getMessage(), e);
        }
    }
}
