package software.spool.crawler.internal.decorator;

import software.spool.core.exception.BusEmitException;
import software.spool.core.exception.SpoolException;
import software.spool.core.model.SpoolEvent;
import software.spool.crawler.api.port.EventBusEmitter;

public class SafeEventBusEmitterEmitter implements EventBusEmitter {
    private final EventBusEmitter bus;

    private SafeEventBusEmitterEmitter(EventBusEmitter bus) {
        this.bus = bus;
    }

    public static SafeEventBusEmitterEmitter of(EventBusEmitter bus) {
        return new SafeEventBusEmitterEmitter(bus);
    }

    @Override
    public void emit(SpoolEvent event) throws BusEmitException {
        try {
            bus.emit(event);
        } catch (SpoolException e) { throw e; } catch (Exception e) {
            throw new BusEmitException("An error occurred while emitting an event: " + e.getMessage(), e);
        }
    }
}
