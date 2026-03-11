package software.spool.crawler.api.builder;

import software.spool.crawler.api.utils.ProcessorFormat;
import software.spool.crawler.api.port.source.PollSource;
import software.spool.crawler.api.strategy.CrawlerStrategy;
import software.spool.crawler.internal.decorator.*;
import software.spool.crawler.internal.strategy.PollCrawlerStrategy;
import software.spool.crawler.api.utils.CrawlerPorts;
import software.spool.crawler.internal.utils.factory.Transformer;

/**
 * Fluent builder that configures a poll-based {@link CrawlerStrategy}.
 *
 * <p>
 * Instances are normally obtained via
 * {@link CrawlerBuilderFactory#poll(PollSource)}
 * and then further customised with the chainable setter methods before calling
 * {@link #create()} to build the strategy.
 * </p>
 *
 * <p>
 * All ports passed to the setter methods are automatically wrapped in their
 * corresponding {@code Safe*} decorators to normalise unchecked exceptions into
 * typed {@link software.spool.core.exception.SpoolException} subclasses.
 * </p>
 *
 * @param <R> the raw type returned by the configured {@link PollSource}
 * @param <T> the intermediate type after deserialization
 * @param <O> the individual record type produced by the splitter
 */
public class PollSourceBuilder<R, T, O> {
    private final PollSource<R> source;
    private Transformer<R, T, O> transformer;
    private CrawlerPorts ports;

    /**
     * Creates a new step wrapping the given source and ports.
     *
     * <p>
     * The source is immediately decorated with {@link SafePollSource} to
     * normalise any unchecked exceptions thrown during polling.
     * </p>
     *
     * @param source the poll source; must not be {@code null}
     */
    public PollSourceBuilder(PollSource<R> source) {
        this.source = SafePollSource.of(source);
    }

    private PollSourceBuilder(PollSource<R> source, CrawlerPorts ports) {
        this.source = SafePollSource.of(source);
        this.ports = ports;
    }

    /**
     * Sets the processing {@link Transformer} (deserializer + splitter +
     * serializer).
     *
     * <p>
     * Each component of the transformer is wrapped in its corresponding
     * {@code Safe*} decorator before being stored.
     * </p>
     *
     * @param transformer the transformer to apply to each polled payload;
     *                    must not be {@code null}
     * @return this step for chaining
     */
    public PollSourceBuilder<R, T, O> transformer(Transformer<R, T, O> transformer) {
        this.transformer = Transformer.of(
                SafePayloadDeserializer.of(transformer.deserializer()),
                SafePayloadSplitter.of(transformer.splitter()),
                SafeRecordSerializer.of(transformer.serializer()));
        return this;
    }

    /**
     * Replaces all ports with the given {@link CrawlerPorts} bundle.
     *
     * @param ports the new ports; must not be {@code null}
     * @return this step for chaining
     */
    public PollSourceBuilder<R, T, O> ports(CrawlerPorts ports) {
        this.ports = ports;
        return this;
    }

    /**
     * Builds and returns the configured {@link CrawlerStrategy}.
     *
     * @return a fully configured {@link CrawlerStrategy} ready to be executed
     */
    public CrawlerStrategy create() {
        return new PollCrawlerStrategy<>(source, transformer, ports);
    }

    /**
     * Applies the given {@link ProcessorFormat} to this step, returning a new
     * step with updated type parameters matching the format's output types.
     *
     * <p>
     * The transformer produced by the format is wrapped in the corresponding
     * {@code Safe*} decorators automatically.
     * </p>
     *
     * @param <NT>   the new intermediate type produced by the format's deserializer
     * @param <NO>   the new record type produced by the format's splitter
     * @param format the processing format to apply; must not be {@code null}
     * @return a new {@link PollSourceBuilder} with the format applied
     */
    public <NT, NO> PollSourceBuilder<R, NT, NO> withFormat(ProcessorFormat<R, NT, NO> format) {
        Transformer<R, NT, NO> pipeline = format.pipeline();
        return new PollSourceBuilder<R, NT, NO>(source, ports)
                .transformer(pipeline);
    }
}
