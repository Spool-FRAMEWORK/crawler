package software.spool.crawler.internal.adapter.input.http;

import software.spool.core.model.vo.MediaType;
import software.spool.core.port.http.Caller;
import software.spool.core.port.http.HttpRequest;
import software.spool.core.port.http.HttpResponse;
import software.spool.core.port.http.HttpRouteHandler;
import software.spool.crawler.api.normalizer.Normalizer;
import software.spool.crawler.internal.control.CaptureOutcome;
import software.spool.crawler.internal.control.PayloadCapture;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Receives one webhook delivery: checks that its body is of the media type of the source, splits it into records with
 * the source's normalizer and captures each one, as a stream crawler does with each message. A record the inbox
 * already has is not stored again, so a delivery sent twice is accepted both times. While the circuit of the inbox is
 * open, a delivery is answered 503 with a Retry-After that says how many seconds to wait.
 */
public final class WebhookRouteHandler implements HttpRouteHandler {
    private static final String CONTENT_TYPE = "Content-Type";
    private static final String PARAMETER_SEPARATOR = ";";
    private static final int ACCEPTED = 202;
    private static final int BAD_REQUEST = 400;
    private static final int UNSUPPORTED_MEDIA_TYPE = 415;
    private static final int SERVICE_UNAVAILABLE = 503;
    private static final String RETRY_AFTER = "Retry-After";
    private static final long MINIMUM_RETRY_AFTER_SECONDS = 1;
    private static final long MILLIS_PER_SECOND = 1000;

    private final Normalizer<byte[]> normalizer;
    private final MediaType mediaType;
    private final PayloadCapture capture;
    private final Clock clock;

    public WebhookRouteHandler(Normalizer<byte[]> normalizer, MediaType mediaType, PayloadCapture capture) {
        this(normalizer, mediaType, capture, Clock.systemUTC());
    }

    public WebhookRouteHandler(Normalizer<byte[]> normalizer, MediaType mediaType, PayloadCapture capture, Clock clock) {
        this.normalizer = normalizer;
        this.mediaType = mediaType;
        this.capture = capture;
        this.clock = clock;
    }

    @Override
    public HttpResponse handle(HttpRequest request, Caller caller) {
        Optional<String> contentType = request.header(CONTENT_TYPE).map(WebhookRouteHandler::withoutParameters);
        if (contentType.filter(withoutParameters(mediaType.value())::equals).isEmpty()) {
            return error(UNSUPPORTED_MEDIA_TYPE, "the body must be " + mediaType.value() + ", got "
                    + request.header(CONTENT_TYPE).orElse("no Content-Type"));
        }
        List<byte[]> records = normalizer.normalize(request.body()).toList();
        if (records.isEmpty()) {
            return error(BAD_REQUEST, "no records could be read from the body as " + mediaType.value());
        }
        int stored = 0;
        int duplicates = 0;
        int rejected = 0;
        for (byte[] record : records) {
            CaptureOutcome outcome = capture.capture(record);
            if (outcome instanceof CaptureOutcome.Waiting waiting) {
                long seconds = secondsUntil(waiting.until());
                return error(SERVICE_UNAVAILABLE, "the inbox takes no deliveries for now; send it again in " + seconds + " s")
                        .withHeader(RETRY_AFTER, String.valueOf(seconds));
            }
            if (outcome == CaptureOutcome.FAILED) {
                return error(SERVICE_UNAVAILABLE, "the delivery could not be stored; send it again");
            }
            if (outcome == CaptureOutcome.STORED) stored++;
            else if (outcome == CaptureOutcome.REJECTED) rejected++;
            else duplicates++;
        }
        return HttpResponse.json(ACCEPTED, "{\"stored\":" + stored + ",\"duplicates\":" + duplicates
                + ",\"rejected\":" + rejected + "}");
    }

    private long secondsUntil(Instant until) {
        long millis = Duration.between(clock.instant(), until).toMillis();
        return Math.max(MINIMUM_RETRY_AFTER_SECONDS, (millis + MILLIS_PER_SECOND - 1) / MILLIS_PER_SECOND);
    }

    private static String withoutParameters(String contentType) {
        return contentType.split(PARAMETER_SEPARATOR, 2)[0].strip().toLowerCase(Locale.ROOT);
    }

    private static HttpResponse error(int status, String message) {
        return HttpResponse.json(status, "{\"error\":\"" + message.replace("\\", "\\\\").replace("\"", "\\\"") + "\"}");
    }
}
