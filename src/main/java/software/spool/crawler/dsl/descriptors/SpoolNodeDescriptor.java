package software.spool.crawler.dsl.descriptors;

import software.spool.crawler.dsl.descriptors.infrastructure.InfrastructureDescriptor;
import software.spool.crawler.dsl.descriptors.module.SpoolModuleDescriptor;

import java.util.List;

public record SpoolNodeDescriptor(
        InfrastructureDescriptor infrastructure,
        List<SpoolModuleDescriptor> spoolModuleList
) {
}
