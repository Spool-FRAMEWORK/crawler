package software.spool.crawler.internal.control;

/**
 * Captures one payload, through the whole pipeline that stores it in the inbox, and says what happened to it.
 */
@FunctionalInterface
public interface PayloadCapture {
    CaptureOutcome capture(byte[] payload);
}
