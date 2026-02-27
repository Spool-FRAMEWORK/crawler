package es.ulpgc.eii.spool.crawler.api;

import es.ulpgc.eii.spool.crawler.api.exception.DeserializationException;

public interface SourceDeserializer<R, T> {
    T deserialize(R source) throws DeserializationException;
}
