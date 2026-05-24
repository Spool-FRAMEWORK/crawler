package software.spool.crawler.api.utils;

import software.spool.core.port.serde.EnrichmentRule;
import software.spool.crawler.internal.utils.factory.Normalizer;
import software.spool.crawler.internal.utils.factory.NormalizerFactory;

import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class StandardNormalizer {

        private StandardNormalizer() {}

        public static final NormalizerFormat<byte[]>    JSON_OBJECT = NormalizerFactory::jsonObject;
        public static final NormalizerFormat<byte[]>    JSON_ARRAY  = NormalizerFactory::jsonArray;
        public static final NormalizerFormat<byte[]>    YAML_ARRAY  = NormalizerFactory::yamlArray;
        public static final NormalizerFormat<ResultSet> RESULT_SET  = (r, p) -> NormalizerFactory.resultSet();
        public static final NormalizerFormat<Object>    IN_MEMORY   = NormalizerFactory::inMemory;

        public static final class Builder {

                private List<EnrichmentRule> enrichRules = new ArrayList<>();
                private String rootPath = "";

                public Builder enrichRules(List<EnrichmentRule> enrichRules) {
                        this.enrichRules = Objects.requireNonNullElse(enrichRules, new ArrayList<>());
                        return this;
                }

                public Builder rootPath(String rootPath) {
                        this.rootPath = Objects.requireNonNullElse(rootPath, "");
                        return this;
                }

                public Normalizer<?> valueOf(Format format) {
                        return switch (format) {
                                case JSON_OBJECT -> JSON_OBJECT.pipelineWith(enrichRules, rootPath);
                                case JSON_ARRAY  -> JSON_ARRAY.pipelineWith(enrichRules, rootPath);
                                case YAML_ARRAY  -> YAML_ARRAY.pipelineWith(enrichRules, rootPath);
                                case RESULT_SET  -> RESULT_SET.pipelineWith(enrichRules, rootPath);
                                case IN_MEMORY   -> IN_MEMORY.pipelineWith(enrichRules, rootPath);
                        };
                }
        }

        public enum Format {
                JSON_ARRAY, YAML_ARRAY, RESULT_SET, JSON_OBJECT, IN_MEMORY
        }
}