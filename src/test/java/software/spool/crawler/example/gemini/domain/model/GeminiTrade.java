package software.spool.crawler.example.gemini.domain.model;

import java.time.Instant;

public record GeminiTrade(
        long tid,
        String price,
        String amount,
        String type,
        Instant occurredAt) {
}
