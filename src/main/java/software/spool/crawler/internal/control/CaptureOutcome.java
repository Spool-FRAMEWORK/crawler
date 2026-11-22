package software.spool.crawler.internal.control;

/**
 * What happened to one captured payload: stored in the inbox, already there, or not stored because something failed.
 */
public enum CaptureOutcome {
    STORED,
    DUPLICATE,
    FAILED
}
