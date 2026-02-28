package software.spool.crawler.api.exception;

public class SourceSplitException extends SpoolException {

    private final String rawInput;

    public SourceSplitException(String message, String rawInput, Throwable cause) {
        super(message, cause);
        this.rawInput = rawInput;
    }

    public SourceSplitException(String message, String rawInput) {
        super(message);
        this.rawInput = rawInput;
    }

    public SourceSplitException(String message) {
        super(message);
        this.rawInput = null;
    }

    public String rawInput() {
        return rawInput;
    }
}
