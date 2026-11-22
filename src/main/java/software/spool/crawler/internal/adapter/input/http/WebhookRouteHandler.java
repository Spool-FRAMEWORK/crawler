package software.spool.crawler.internal.adapter.input.http;

import software.spool.core.model.vo.MediaType;
import software.spool.core.port.http.Caller;
import software.spool.core.port.http.HttpRequest;
import software.spool.core.port.http.HttpResponse;
import software.spool.core.port.http.HttpRouteHandler;
import software.spool.crawler.api.normalizer.Normalizer;
import software.spool.crawler.internal.control.CaptureOutcome;
import software.spool.crawler.internal.control.PayloadCapture;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Receives one webhook delivery: checks that its body is of the media type of the source, splits it into records with
 * the source's normalizer and captures each one, as a stream crawler does with each message. A record the inbox
 * already has is not stored again, so a delivery sent twice is accepted both times.
 */
public final class WebhookRouteHandler implements HttpRouteHandler {
    private static final String CONTENT_TYPE = "Content-Type";
    private static final String PARAMETER_SEPARATOR = ";";
    private static final int ACCEPTED = 202;
    private static final int BAD_REQUEST = 400;
    private static final int UNSUPPORTED_MEDIA_TYPE = 415;
    private static final int SERVICE_UNAVAILABLE = 503;

    private final Normalizer<byte[]> normalizer;
    private final MediaType mediaType;
    private final PayloadCapture capture;

    public WebhookRouteHandler(Normalizer<byte[]> normalizer, MediaType mediaType, PayloadCapture capture) {
        this.normalizer = normalizer;
        this.mediaType = mediaType;
        this.capture = capture;
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
        for (byte[] record : records) {
            CaptureOutcome outcome = capture.capture(record);
            if (outcome == CaptureOutcome.FAILED) {
                return error(SERVICE_UNAVAILABLE, "the delivery could not be stored; send it again");
            }
            if (outcome == CaptureOutcome.STORED) stored++;
            else duplicates++;
        }
        return HttpResponse.json(ACCEPTED, "{\"stored\":" + stored + ",\"duplicates\":" + duplicates + "}");
    }

    private static String withoutParameters(String contentType) {
        return contentType.split(PARAMETER_SEPARATOR, 2)[0].strip().toLowerCase(Locale.ROOT);
    }

    private static HttpResponse error(int status, String message) {
        return HttpResponse.json(status, "{\"error\":\"" + message.replace("\\", "\\\\").replace("\"", "\\\"") + "\"}");
    }
}
