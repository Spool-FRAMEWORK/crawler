package software.spool.crawler.api.exception;

public class BusEmitException extends SpoolException {
    public BusEmitException(String message) {
        super(message);
    }
    public BusEmitException(String message, Throwable cause) {
        super(message, cause);
    }
}
