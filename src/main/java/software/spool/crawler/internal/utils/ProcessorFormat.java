package software.spool.crawler.internal.utils;

import software.spool.crawler.internal.utils.factory.Transformer;

public interface ProcessorFormat<R, P, T> {
    Transformer<R, P, T> pipeline();
}
