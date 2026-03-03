package software.spool.crawler.api.port;

import software.spool.core.exception.InboxWriteException;
import software.spool.core.model.SourceItemCaptured;

public interface InboxWriter {
    String receive(String payload, String idempotencyKey) throws InboxWriteException;
}
