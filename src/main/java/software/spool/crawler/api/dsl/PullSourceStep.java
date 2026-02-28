package software.spool.crawler.api.dsl;

import software.spool.crawler.api.source.PullSource;
import software.spool.crawler.api.strategy.CrawlerStrategy;
import software.spool.crawler.api.strategy.PullCrawlerStrategy;
import software.spool.crawler.internal.utils.CrawlerPorts;
import software.spool.crawler.internal.utils.ProcessorFormat;
import software.spool.crawler.internal.utils.factory.Transformer;

public class PullSourceStep<R, T, O> {
    private final PullSource<R> source;
    private Transformer<R, T, O> transformer;
    private CrawlerPorts ports;
    private String sender;

    public PullSourceStep(PullSource<R> source, CrawlerPorts ports) {
        this.source = source;
        this.ports = ports;
    }

    public PullSourceStep<R, T, O> transformer(Transformer<R, T, O> transformer) {
        this.transformer = transformer;
        return this;
    }

    public PullSourceStep<R, T, O> ports(CrawlerPorts ports) {
        this.ports = ports;
        return this;
    }

    public PullSourceStep<R, T, O> senderName(String sender) {
        this.sender = sender;
        return this;
    }

    public CrawlerStrategy create() {
        return new PullCrawlerStrategy<>(source, transformer, ports, sender);
    }

    public <NT, NO> PullSourceStep<R, NT, NO> withFormat(ProcessorFormat<R, NT, NO> format) {
        Transformer<R, NT, NO> pipeline = format.pipeline();
        return new PullSourceStep<R, NT, NO>(source, ports)
                .transformer(pipeline);
    }
}
