package software.spool.crawler.internal.port;

import software.spool.crawler.api.exception.InboxWriteException;
import software.spool.crawler.api.source.InboxEntryId;
import software.spool.model.RawDataReadFromSource;
import software.spool.crawler.api.exception.SpoolException;

@FunctionalInterface
public interface InboxWriter {
    InboxEntryId receive(RawDataReadFromSource event) throws InboxWriteException;
}