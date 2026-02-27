package es.ulpgc.eii.spool.crawler.internal.utils;

import es.ulpgc.eii.spool.crawler.api.SourceDeserializer;
import es.ulpgc.eii.spool.crawler.api.SourceSplitter;
import es.ulpgc.eii.spool.crawler.api.SourceSerializer;

import java.util.stream.Stream;

public record Transformer<R, P, T>(
    SourceDeserializer<R, P> deserializer,
    SourceSplitter<P, T> splitter,
    SourceSerializer<T> serializer
) {
    public static <T> Transformer<Object, Object, T> onlySerializer(SourceSerializer<T> serializer) {
        return new Transformer<>(r -> null, (p, source) -> Stream.of((T)p), serializer);
    }
    
    public static <R, P> Transformer<R, P, P> noSplitter(SourceDeserializer<R, P> deserializer, SourceSerializer<P> serializer) {
        return new Transformer<>(deserializer, (p, source) -> Stream.of(p), serializer);
    }
}
