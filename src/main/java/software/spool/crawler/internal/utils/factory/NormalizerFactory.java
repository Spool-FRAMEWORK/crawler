package software.spool.crawler.internal.utils.factory;

import software.spool.core.adapter.jackson.*;
import software.spool.core.pipeline.ObservedStep;
import software.spool.core.pipeline.Pipeline;
import software.spool.core.port.serde.*;
import software.spool.crawler.internal.utils.factory.steps.*;

import java.sql.ResultSet;
import java.util.List;
import java.util.stream.Stream;

public final class NormalizerFactory {

    private NormalizerFactory() {}

    public static Normalizer<byte[]> jsonArray(List<EnrichmentRule> rules, String rootPath) {
        return new Normalizer<>(Pipeline.<byte[]>start()
                .add(new ObservedStep<>("deserialize",  new DeserializeStep<>(PayloadDeserializerFactory.json().asNode())))
                .add(new ObservedStep<>("locate",       new LocateStep<>(PayloadLocatorFactory.fromRootPath(rootPath))))
                .add(new ObservedStep<>("split-enrich", new SplitEnrichStep<>(PayloadSplitterFactory.jsonArray(), PayloadExtractorFactory.withRules(rules), RecordEnricherFactory.json())))
                .add(new ObservedStep<>("serialize",    new SerializeStep<>(RecordSerializerFactory.jsonNode()))));
    }

    public static Normalizer<byte[]> yamlArray(List<EnrichmentRule> rules, String rootPath) {
        return new Normalizer<>(Pipeline.<byte[]>start()
                .add(new ObservedStep<>("deserialize",  new DeserializeStep<>(PayloadDeserializerFactory.yaml().asNode())))
                .add(new ObservedStep<>("locate",       new LocateStep<>(PayloadLocatorFactory.fromRootPath(rootPath))))
                .add(new ObservedStep<>("split-enrich", new SplitEnrichStep<>(PayloadSplitterFactory.jsonArray(), PayloadExtractorFactory.withRules(rules), RecordEnricherFactory.json())))
                .add(new ObservedStep<>("serialize",    new SerializeStep<>(RecordSerializerFactory.jsonNode()))));
    }

    public static Normalizer<byte[]> jsonObject(List<EnrichmentRule> rules, String rootPath) {
        return new Normalizer<>(Pipeline.<byte[]>start()
                .add(new ObservedStep<>("deserialize",  new DeserializeStep<>(PayloadDeserializerFactory.json().asNode())))
                .add(new ObservedStep<>("locate",       new LocateStep<>(PayloadLocatorFactory.fromRootPath(rootPath))))
                .add(new ObservedStep<>("split-enrich", new SplitEnrichStep<>(PayloadSplitterFactory.single(), PayloadExtractorFactory.withRules(rules), RecordEnricherFactory.json())))
                .add(new ObservedStep<>("serialize",    new SerializeStep<>(RecordSerializerFactory.jsonNode()))));
    }

    public static Normalizer<ResultSet> resultSet() {
        return new Normalizer<>(Pipeline.<ResultSet>start()
                .add(new ObservedStep<>("split-enrich", new SplitEnrichStep<>(PayloadSplitterFactory.resultSet(), PayloadExtractorFactory.noOp(), RecordEnricherFactory.noOp())))
                .add(new ObservedStep<>("serialize",    new SerializeStep<>(RecordSerializerFactory.map()))));
    }

    public static <I> Normalizer<I> inMemory(List<EnrichmentRule> rules, String rootPath) {
        return new Normalizer<>(Pipeline.<I>start()
                .add(new ObservedStep<>("map-to-json",  new MapStep<>(PayloadMapperFactory.jsonNode())))
                .add(new ObservedStep<>("locate",       new LocateStep<>(PayloadLocatorFactory.fromRootPath(rootPath))))
                .add(new ObservedStep<>("split-enrich", new SplitEnrichStep<>(PayloadSplitterFactory.single(), PayloadExtractorFactory.withRules(rules), RecordEnricherFactory.json())))
                .add(new ObservedStep<>("serialize",    new SerializeStep<>(RecordSerializerFactory.jsonNode()))));
    }

    public static <I> Normalizer<I> of(Pipeline<I, Stream<byte[]>> pipeline) {
        return new Normalizer<>(pipeline);
    }
}