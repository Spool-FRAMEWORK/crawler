package software.spool.crawler.api.exception;

public class InboxWriteException extends SpoolException {
    public InboxWriteException(String message) {
        super(message);
    }
    public InboxWriteException(String message, Throwable cause) {
        super(message, cause);
    }
}
