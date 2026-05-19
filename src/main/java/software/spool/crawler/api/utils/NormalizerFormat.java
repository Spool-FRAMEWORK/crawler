package software.spool.crawler.api.utils;

import software.spool.core.port.serde.EnrichmentRule;
import software.spool.crawler.internal.utils.factory.Normalizer;

import java.util.List;

@FunctionalInterface
public interface NormalizerFormat<P, E, R> {
    Normalizer<P, E, R> pipelineWith(List<EnrichmentRule> enrichmentRules, String rootPath);
}
