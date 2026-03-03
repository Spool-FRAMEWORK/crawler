package software.spool.crawler.internal.decorator;

import software.spool.core.exception.InboxWriteException;
import software.spool.core.exception.SpoolException;
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
    public String receive(String payload, String idempotencyKey) throws InboxWriteException {
        try {
            return inbox.receive(payload, idempotencyKey);
        } catch (SpoolException e) { throw e; } catch (Exception e) {
            throw new InboxWriteException("Failed while writing to inbox: " + e.getMessage(), e);
        }
    }
}
