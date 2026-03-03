package software.spool.crawler.api;

import software.spool.core.model.SpoolEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

public class ErrorRouter {

    private record Entry<E extends Exception>(Class<E> type, BiConsumer<E, SpoolEvent> handler) {
        @SuppressWarnings("unchecked")
        boolean tryHandle(Exception e, SpoolEvent context) {
            if (type.isInstance(e)) { handler.accept((E) e, context); return true; }
            return false;
        }
    }

    private final List<Entry<?>> entries = new ArrayList<>();
    private BiConsumer<Exception, SpoolEvent> fallback = (e, cause) -> {};

    public <E extends Exception> ErrorRouter on(Class<E> type, BiConsumer<E, SpoolEvent> handler) {
        entries.add(new Entry<>(type, handler));
        return this;
    }

    public ErrorRouter orElse(BiConsumer<Exception, SpoolEvent> fallback) {
        this.fallback = fallback;
        return this;
    }

    public void dispatch(Exception exception) {
        dispatch(exception, null);
    }

    public void dispatch(Exception exception, SpoolEvent context) {
        entries.stream()
               .filter(entry -> entry.tryHandle(exception, context))
               .findFirst()
               .orElseGet(() -> { fallback.accept(exception, context); return null; });
    }
}
