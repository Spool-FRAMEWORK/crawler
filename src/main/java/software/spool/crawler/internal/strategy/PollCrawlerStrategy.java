package software.spool.crawler.internal.strategy;

import software.spool.core.exception.*;
import software.spool.crawler.api.strategy.BaseCrawlerStrategy;
import software.spool.crawler.api.strategy.CrawlerStrategy;
import software.spool.crawler.internal.utils.CrawlerPorts;
import software.spool.crawler.internal.utils.factory.Transformer;
import software.spool.core.model.*;
import software.spool.crawler.api.source.PollSource;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

public class PollCrawlerStrategy<R, T, O> extends BaseCrawlerStrategy implements CrawlerStrategy {
    private final PollSource<R> source;
    private final Transformer<R, T, O> transformer;
    private final CrawlerPorts ports;
    private final String sender;

    public PollCrawlerStrategy(PollSource<R> source, Transformer<R, T, O> transformer, CrawlerPorts ports, String sender) {
        super(ports.bus(), source.sourceId(), sender, ports.errorRouter());
        this.source = source;
        this.transformer = transformer;
        this.ports = ports;
        this.sender = sender;
    }

    @Override
    public void execute() throws SpoolException {
        try (PollSource<R> source = this.source.open()) {
            transformer.splitter().split(transformer.deserializer().deserialize(source.poll()), source.sourceId())
                    .forEach(this::process);
        } catch (SpoolContextException e) {
            errorRouter.dispatch(e, e.context());
        } catch (Exception e) {
            errorRouter.dispatch(e);
        }
    }

    private void process(O record) {
        String payload = transformer.serializer().serialize(record, sender);
        SourceItemCaptured itemCapturedEvent = SourceItemCaptured.builder()
                .senderId(sender)
                .sourceId(source.sourceId())
                .idempotencyKey(generateIdempotencyKeyFrom(payload))
                .build();
        try {
            ports.inboxWriter().receive(payload, itemCapturedEvent.idempotencyKey());
            ports.bus().emit(itemCapturedEvent);
            ports.bus().emit(InboxItemStored.builder().from(itemCapturedEvent).build());
        } catch (Exception e) {
            throw new SpoolContextException(e, itemCapturedEvent);
        }
    }

    private String generateIdempotencyKeyFrom(String payload) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            String input = source.sourceId() + ":" + payload;
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
