package es.ulpgc.eii.spool.crawler.dsl;

import es.ulpgc.eii.spool.crawler.api.source.PullSource;
import es.ulpgc.eii.spool.crawler.internal.utils.InMemoryInbox;

public final class Crawlers {
    private Crawlers() {}

    public static <R> PullSourceStep<R, R, R> pull(PullSource<R> source) {
        return new PullSourceStep<>(source, new InMemoryInbox(), System.out::println);
    }
}
