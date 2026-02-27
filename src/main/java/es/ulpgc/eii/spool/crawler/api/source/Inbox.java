package es.ulpgc.eii.spool.crawler.api.source;

import es.ulpgc.eii.spool.core.model.RawInboxEvent;
import es.ulpgc.eii.spool.crawler.api.exception.SpoolException;

@FunctionalInterface
public interface Inbox {
    InboxEntryId receive(RawInboxEvent event) throws SpoolException;
}