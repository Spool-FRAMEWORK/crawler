package software.spool.crawler.api.exception;

public class SourcePollException extends SpoolException {
    public SourcePollException(String message) {
        super(message);
    }
    public SourcePollException(String message, Throwable cause) {
        super(message, cause);
    }
}
