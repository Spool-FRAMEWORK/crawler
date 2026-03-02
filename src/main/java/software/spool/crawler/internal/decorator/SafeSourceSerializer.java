package software.spool.crawler.internal.decorator;

import software.spool.core.exception.SerializationException;
import software.spool.core.exception.SpoolException;
import software.spool.crawler.internal.port.SourceSerializer;

public class SafeSourceSerializer<T> implements SourceSerializer<T> {
    private final SourceSerializer<T> serializer;

    private SafeSourceSerializer(SourceSerializer<T> serializer) {
        this.serializer = serializer;
    }

    public static <T> SafeSourceSerializer<T> of(SourceSerializer<T> serializer) {
        return new SafeSourceSerializer<>(serializer);
    }

    @Override
    public String serialize(T record, String sourceId) throws SpoolException {
        try {
            return serializer.serialize(record, sourceId);
        } catch (SpoolException e) { throw e; } catch (Exception e) {
            throw new SerializationException("Error while serializing: " + e.getMessage(), record.toString());
        }
    }
}
