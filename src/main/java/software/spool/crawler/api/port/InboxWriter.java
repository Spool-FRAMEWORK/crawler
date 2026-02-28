package software.spool.crawler.api.port;

import software.spool.crawler.api.exception.InboxWriteException;
import software.spool.model.RawDataReadFromSource;

public interface InboxWriter {
    InboxEntryId receive(RawDataReadFromSource event) throws InboxWriteException;
}
