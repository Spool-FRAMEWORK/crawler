package software.spool.crawler.api.normalizer;

/** A normalizer that cannot be put together as asked: a stage it lacks, or steps whose types do not fit. */
public class NormalizerCompositionException extends RuntimeException {

    public NormalizerCompositionException(String message) {
        super(message);
    }

    public NormalizerCompositionException(String message, Throwable cause) {
        super(message, cause);
    }
}
