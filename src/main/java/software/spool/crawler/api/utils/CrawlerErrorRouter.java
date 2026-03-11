package software.spool.crawler.api.utils;

import software.spool.core.exception.*;
import software.spool.core.model.*;
import software.spool.core.port.EventBusEmitter;
import software.spool.core.utils.ErrorRouter;

public class CrawlerErrorRouter {

    public static ErrorRouter defaults(EventBusEmitter bus) {
        return new ErrorRouter()
            .on(SourceOpenException.class,
                (e, cause) -> bus.emit(SourceFetchFailed.builder()
                    .errorMessage(e.getMessage()).build()))
            .on(SourcePollException.class,
                (e, cause) -> bus.emit(SourceFetchFailed.builder()
                    .errorMessage(e.getMessage()).build()))
            .on(DeserializationException.class,
                (e, cause) -> bus.emit(SourceItemCaptureFailed.builder()
                    .errorMessage(e.getMessage()).build()))
            .on(SourceSplitException.class,
                (e, cause) -> bus.emit(SourceItemCaptureFailed.builder()
                    .errorMessage(e.getMessage()).build()))
            .on(SerializationException.class,
                (e, cause) -> bus.emit(SourceItemCaptureFailed.builder()
                    .errorMessage(e.getMessage()).build()))
            .on(InboxWriteException.class,
                (e, cause) -> bus.emit(InboxItemStoreFailed.builder()
                    .from(cause).errorMessage(e.getMessage()).build()));
    }
}
