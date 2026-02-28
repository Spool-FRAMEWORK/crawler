package software.spool.crawler.api.port;

import software.spool.crawler.api.exception.BusEmitException;
import software.spool.model.SpoolEvent;

public interface EventBus {
    void emit(SpoolEvent event) throws BusEmitException;
}
