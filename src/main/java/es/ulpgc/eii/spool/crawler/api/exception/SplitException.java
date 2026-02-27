package es.ulpgc.eii.spool.crawler.api.exception;

public class SplitException extends SpoolException {
    public SplitException(String message) {
        super(message);
    }
    public SplitException(String message, Throwable cause) {
        super(message, cause);
    }
}
