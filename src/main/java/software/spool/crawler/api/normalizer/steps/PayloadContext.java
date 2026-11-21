package software.spool.crawler.api.normalizer.steps;

import software.spool.core.port.serde.ExtractedField;
import java.util.List;

public record PayloadContext<P, E>(P payload, List<ExtractedField<E>> enrichment) {}
