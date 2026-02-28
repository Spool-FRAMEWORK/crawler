package software.spool.crawler.internal.utils.factory;

import software.spool.crawler.internal.port.SourceDeserializer;
import software.spool.crawler.internal.port.SourceSplitter;
import software.spool.crawler.internal.port.SourceSerializer;

import java.util.stream.Stream;

public record Transformer<R, P, T>(
    SourceDeserializer<R, P> deserializer,
    SourceSplitter<P, T> splitter,
    SourceSerializer<T> serializer
) {
    public static <R, P, T> Transformer<R, P, T> of(SourceDeserializer<R, P> deserializer, SourceSplitter<P, T> splitter, SourceSerializer<T> serializer) {
        return new Transformer<>(deserializer, splitter, serializer);
    }

    public static <T> Transformer<Object, Object, T> onlySerializer(SourceSerializer<T> serializer) {
        return new Transformer<>(r -> null, (p, source) -> Stream.of((T)p), serializer);
    }
    
    public static <R, P> Transformer<R, P, P> noSplitter(SourceDeserializer<R, P> deserializer, SourceSerializer<P> serializer) {
        return new Transformer<>(deserializer, (p, source) -> Stream.of(p), serializer);
    }
}
