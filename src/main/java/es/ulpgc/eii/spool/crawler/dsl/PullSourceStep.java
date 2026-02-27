package es.ulpgc.eii.spool.crawler.dsl;

import es.ulpgc.eii.spool.crawler.api.*;
import es.ulpgc.eii.spool.crawler.api.source.Inbox;
import es.ulpgc.eii.spool.crawler.api.source.PullSource;
import es.ulpgc.eii.spool.crawler.api.strategy.CrawlerStrategy;
import es.ulpgc.eii.spool.crawler.api.strategy.PullCrawlerStrategy;
import es.ulpgc.eii.spool.crawler.internal.utils.*;

public class PullSourceStep<R, T, O> {
    private final PullSource<R> source;
    private SourceDeserializer<R, T> deserializer;
    private SourceSplitter<T, O> splitter;
    private SourceSerializer<O> serializer;
    private Inbox inbox;
    private EventBus bus;

    public PullSourceStep(PullSource<R> source, Inbox inbox, EventBus bus) {
        this.source = source;
        this.inbox = inbox;
        this.bus = bus;
    }

    public PullSourceStep<R, T, O> splitWith(SourceSplitter<T, O> splitter) {
        this.splitter = splitter;
        return this;
    }

    public PullSourceStep<R, T, O> serializeWith(SourceSerializer<O> serializer) {
        this.serializer = serializer;
        return this;
    }

    public PullSourceStep<R, T, O> deserializeWith(SourceDeserializer<R, T> deserializer) {
        this.deserializer = deserializer;
        return this;
    }

    public PullSourceStep<R, T, O> inbox(Inbox inbox) {
        this.inbox = inbox;
        return this;
    }

    public PullSourceStep<R, T, O> bus(EventBus bus) {
        this.bus = bus;
        return this;
    }

    public CrawlerStrategy create() {
        return new PullCrawlerStrategy<>(source, deserializer, splitter, serializer, inbox, bus);
    }

    public <NT, NO> PullSourceStep<R, NT, NO> splitWith(
            ProcessorFormat<R, NT, NO> format) {

        Transformer<R, NT, NO> pipeline = format.pipeline();

        return new PullSourceStep<R, NT, NO>(source, inbox, bus)
                .deserializeWith(pipeline.deserializer())
                .splitWith(pipeline.splitter())
                .serializeWith(pipeline.serializer());
    }
}
