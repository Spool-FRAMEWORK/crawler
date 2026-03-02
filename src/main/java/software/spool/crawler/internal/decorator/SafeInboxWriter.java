package software.spool.crawler.internal.decorator;

import software.spool.core.exception.InboxWriteException;
import software.spool.core.model.RawDataReadFromSource;
import software.spool.crawler.api.port.InboxEntryId;
import software.spool.crawler.api.port.InboxWriter;

public class SafeInboxWriter implements InboxWriter {
    private final InboxWriter inbox;


    private SafeInboxWriter(InboxWriter inbox) {
        this.inbox = inbox;
    }

    public static SafeInboxWriter of(InboxWriter inbox) {
        return new SafeInboxWriter(inbox);
    }

    @Override
    public InboxEntryId receive(RawDataReadFromSource event) throws InboxWriteException {
        try {
            return inbox.receive(event);
        } catch (Exception e) {
            throw new InboxWriteException("Failed while writing to inbox: " + e.getMessage(), e);
        }
    }
}
