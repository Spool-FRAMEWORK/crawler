package software.spool.crawler.api.port;

/**
 * What a source does with a capture whose key is in quarantine in the inbox (ADR-032).
 */
public enum WhenQuarantined {
    /** The capture is refused as a duplicate, and the quarantine is resolved by hand. */
    KEEP,
    /** A capture with another payload takes the place of the one in quarantine; the same payload is a duplicate. */
    REPLACE
}
