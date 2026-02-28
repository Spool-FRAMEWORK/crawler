package software.spool.crawler.api.exception;

public class SerializationException extends SpoolException {

    private final String failedPayload;

    public SerializationException(String message, String failedPayload, Throwable cause) {
        super(message, cause);
        this.failedPayload = failedPayload;
    }

    public SerializationException(String message, String failedPayload) {
        super(message);
        this.failedPayload = failedPayload;
    }

    public String failedPayload() {
        return failedPayload;
    }
}
