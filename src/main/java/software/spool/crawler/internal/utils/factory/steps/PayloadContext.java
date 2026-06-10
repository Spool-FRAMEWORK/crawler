package software.spool.crawler.internal.utils.factory.steps;

import software.spool.core.port.serde.ExtractedField;
import java.util.List;

record PayloadContext<P, E>(P payload, List<ExtractedField<E>> enrichment) {}
