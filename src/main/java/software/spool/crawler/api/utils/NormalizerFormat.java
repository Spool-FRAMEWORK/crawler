package software.spool.crawler.api.utils;

import software.spool.core.port.serde.EnrichmentRule;
import software.spool.crawler.internal.utils.factory.Normalizer;

import java.util.List;

@FunctionalInterface
public interface NormalizerFormat<P> {
    Normalizer<P> pipelineWith(List<EnrichmentRule> enrichmentRules, String rootPath);
}
