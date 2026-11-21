package software.spool.crawler.api.normalizer;

import com.fasterxml.jackson.databind.JsonNode;
import software.spool.core.adapter.jackson.PayloadDeserializerFactory;
import software.spool.core.adapter.jackson.PayloadExtractorFactory;
import software.spool.core.adapter.jackson.PayloadLocatorFactory;
import software.spool.core.adapter.jackson.PayloadMapperFactory;
import software.spool.core.adapter.jackson.RecordEnricherFactory;
import software.spool.core.adapter.jackson.RecordSerializerFactory;
import software.spool.core.port.serde.EnrichmentRule;
import software.spool.core.port.serde.PayloadDeserializer;
import software.spool.crawler.api.normalizer.steps.ContextLocateStep;
import software.spool.crawler.api.normalizer.steps.DeserializeStep;
import software.spool.crawler.api.normalizer.steps.ExtractStep;
import software.spool.crawler.api.normalizer.steps.MapStep;
import software.spool.crawler.api.normalizer.steps.PayloadContext;
import software.spool.crawler.api.normalizer.steps.SerializeStep;
import software.spool.crawler.api.normalizer.steps.SplitEnrichStep;
import software.spool.crawler.api.normalizer.steps.SplitStep;
import software.spool.crawler.api.port.PayloadSplitter;

import java.sql.ResultSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * The normalizers the framework brings, as plans that can be extended at the stages they publish. The JSON and
 * YAML ones share their stages:
 *
 * <pre>
 * [raw] deserialize [deserialized] extract [extracted] context-locate [located] split-enrich [split] serialize [serialized]
 * </pre>
 */
public final class StandardNormalizers {

    /** The document the source fetched, parsed. */
    public static final Stage<JsonNode> DESERIALIZED = new Stage<>("deserialized") {};

    /** The document with the fields its enrichment rules take out of it. */
    public static final Stage<PayloadContext<JsonNode, JsonNode>> EXTRACTED = new Stage<>("extracted") {};

    /** The part of the document the records come from, at the root path. */
    public static final Stage<PayloadContext<JsonNode, JsonNode>> LOCATED = new Stage<>("located") {};

    /** The records, one JSON node each, before they are written as bytes. */
    public static final Stage<Stream<JsonNode>> SPLIT = new Stage<>("split") {};

    private static final Stage<ResultSet> RESULT_SET = new Stage<>("raw") {};
    private static final Stage<Stream<Map<String, Object>>> ROWS = new Stage<>("split") {};
    private static final Stage<Object> OBJECT = new Stage<>("raw") {};
    private static final Stage<JsonNode> MAPPED = new Stage<>("mapped") {};

    private StandardNormalizers() {}

    /** A JSON array: one record per element, at the root path. */
    public static NormalizerPlan<byte[], Stream<byte[]>> jsonArray(List<EnrichmentRule> rules, String rootPath) {
        return structured(PayloadDeserializerFactory.json().asNode(), PayloadSplitterFactory.jsonArray(), rules, rootPath);
    }

    /** A YAML sequence: one record per element, at the root path. */
    public static NormalizerPlan<byte[], Stream<byte[]>> yamlArray(List<EnrichmentRule> rules, String rootPath) {
        return structured(PayloadDeserializerFactory.yaml().asNode(), PayloadSplitterFactory.jsonArray(), rules, rootPath);
    }

    /** A JSON object: the document at the root path is the one record. */
    public static NormalizerPlan<byte[], Stream<byte[]>> jsonObject(List<EnrichmentRule> rules, String rootPath) {
        return structured(PayloadDeserializerFactory.json().asNode(), PayloadSplitterFactory.single(), rules, rootPath);
    }

    /** The rows of a JDBC result: one record per row, keyed by column label. */
    public static NormalizerPlan<ResultSet, Stream<byte[]>> resultSet() {
        return NormalizerPlan.from(RESULT_SET)
                .then(ROWS, "split", new SplitStep<>(PayloadSplitterFactory.resultSet()))
                .then(Stage.SERIALIZED, "serialize", new SerializeStep<>(RecordSerializerFactory.map()));
    }

    /** An object already in memory: it is mapped to JSON and is the one record. */
    public static NormalizerPlan<Object, Stream<byte[]>> inMemory(List<EnrichmentRule> rules, String rootPath) {
        return NormalizerPlan.from(OBJECT)
                .then(MAPPED, "map-to-json", new MapStep<>(PayloadMapperFactory.jsonNode()))
                .then(EXTRACTED, "extract", new ExtractStep<>(PayloadExtractorFactory.withRules(rules)))
                .then(LOCATED, "context-locate", new ContextLocateStep<>(PayloadLocatorFactory.fromRootPath(rootPath)))
                .then(SPLIT, "split-enrich", new SplitEnrichStep<>(PayloadSplitterFactory.single(), RecordEnricherFactory.json()))
                .then(Stage.SERIALIZED, "serialize", new SerializeStep<>(RecordSerializerFactory.jsonNode()));
    }

    private static NormalizerPlan<byte[], Stream<byte[]>> structured(PayloadDeserializer<JsonNode> deserializer,
                                                                     PayloadSplitter<JsonNode, JsonNode> splitter,
                                                                     List<EnrichmentRule> rules, String rootPath) {
        return NormalizerPlan.from(Stage.RAW)
                .then(DESERIALIZED, "deserialize", new DeserializeStep<>(deserializer))
                .then(EXTRACTED, "extract", new ExtractStep<>(PayloadExtractorFactory.withRules(rules)))
                .then(LOCATED, "context-locate", new ContextLocateStep<>(PayloadLocatorFactory.fromRootPath(rootPath)))
                .then(SPLIT, "split-enrich", new SplitEnrichStep<>(splitter, RecordEnricherFactory.json()))
                .then(Stage.SERIALIZED, "serialize", new SerializeStep<>(RecordSerializerFactory.jsonNode()));
    }
}
