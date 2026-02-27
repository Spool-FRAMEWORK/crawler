package es.ulpgc.eii.spool.crawler.api.exception;

public class SerializationException extends SpoolException {
    public SerializationException(String message) {
        super(message);
    }
    public SerializationException(String message, Throwable cause) {
        super(message, cause);
    }
}
