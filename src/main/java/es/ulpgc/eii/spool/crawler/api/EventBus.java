package es.ulpgc.eii.spool.crawler.api;

import es.ulpgc.eii.spool.core.model.SpoolEvent;

public interface EventBus {
    void emit(SpoolEvent event);
}
