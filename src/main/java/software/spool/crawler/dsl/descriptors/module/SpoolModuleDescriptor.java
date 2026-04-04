package software.spool.crawler.dsl.descriptors.module;

import software.spool.crawler.dsl.descriptors.module.crawler.CrawlerDescriptor;

public record SpoolModuleDescriptor(
        CrawlerDescriptor crawler
) {
}
