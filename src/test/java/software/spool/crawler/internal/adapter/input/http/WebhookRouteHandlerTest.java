package software.spool.crawler.internal.adapter.input.http;

import org.junit.jupiter.api.Test;
import software.spool.core.model.vo.MediaType;
import software.spool.core.port.http.Caller;
import software.spool.core.port.http.HttpMethod;
import software.spool.core.port.http.HttpRequest;
import software.spool.core.port.http.HttpResponse;
import software.spool.crawler.api.utils.StandardNormalizer;
import software.spool.crawler.internal.control.CaptureOutcome;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class WebhookRouteHandlerTest {
    private static final MediaType JSON = MediaType.of("application/json");

    private final List<String> captured = new ArrayList<>();

    @Test
    void handle_jsonArray_capturesEachRecordAndAcceptsTheDelivery() {
        HttpResponse response = handlerCapturingAs(CaptureOutcome.STORED)
                .handle(request("application/json", "[{\"id\":\"a-1\"},{\"id\":\"a-2\"}]"), Caller.anonymous());

        assertThat(response.status()).isEqualTo(202);
        assertThat(captured).hasSize(2);
        assertThat(new String(response.body(), StandardCharsets.UTF_8)).isEqualTo("{\"stored\":2,\"duplicates\":0}");
    }

    @Test
    void handle_recordsTheInboxAlreadyHas_acceptsTheDeliveryAndCountsThemAsDuplicates() {
        HttpResponse response = handlerCapturingAs(CaptureOutcome.DUPLICATE)
                .handle(request("application/json", "[{\"id\":\"a-1\"}]"), Caller.anonymous());

        assertThat(response.status()).isEqualTo(202);
        assertThat(new String(response.body(), StandardCharsets.UTF_8)).isEqualTo("{\"stored\":0,\"duplicates\":1}");
    }

    @Test
    void handle_contentTypeWithParameters_isTheMediaTypeOfTheSource() {
        assertThat(handlerCapturingAs(CaptureOutcome.STORED)
                .handle(request("Application/JSON; charset=utf-8", "[{\"id\":\"a-1\"}]"), Caller.anonymous()).status())
                .isEqualTo(202);
    }

    @Test
    void handle_anotherContentType_isRefusedWithoutCapturing() {
        HttpResponse response = handlerCapturingAs(CaptureOutcome.STORED)
                .handle(request("text/csv", "id\na-1\n"), Caller.anonymous());

        assertThat(response.status()).isEqualTo(415);
        assertThat(captured).isEmpty();
    }

    @Test
    void handle_bodyWithoutRecords_isRefusedAsABadRequest() {
        HttpResponse response = handlerCapturingAs(CaptureOutcome.STORED)
                .handle(request("application/json", "not json"), Caller.anonymous());

        assertThat(response.status()).isEqualTo(400);
        assertThat(captured).isEmpty();
    }

    @Test
    void handle_inboxThatFails_answersThatTheDeliveryMustBeSentAgain() {
        HttpResponse response = handlerCapturingAs(CaptureOutcome.FAILED)
                .handle(request("application/json", "[{\"id\":\"a-1\"}]"), Caller.anonymous());

        assertThat(response.status()).isEqualTo(503);
    }

    private WebhookRouteHandler handlerCapturingAs(CaptureOutcome outcome) {
        return new WebhookRouteHandler(StandardNormalizer.JSON_ARRAY.pipelineWith(List.of(), null), JSON, payload -> {
            captured.add(new String(payload, StandardCharsets.UTF_8));
            return outcome;
        });
    }

    private static HttpRequest request(String contentType, String body) {
        return new HttpRequest(HttpMethod.POST, "/spool/webhooks/boe", Map.of("Content-Type", List.of(contentType)),
                body.getBytes(StandardCharsets.UTF_8));
    }
}
