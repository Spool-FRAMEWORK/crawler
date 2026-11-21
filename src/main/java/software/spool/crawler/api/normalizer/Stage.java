package software.spool.crawler.api.normalizer;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * A named point between two steps of a normalizer, with the full type of what passes through it, generics
 * included. A stage is declared as an anonymous subclass so that its type survives compilation:
 *
 * <pre>{@code
 * public static final Stage<Stream<JsonNode>> SPLIT = new Stage<>("split") {};
 * }</pre>
 *
 * @param <T> what passes through the stage
 */
public abstract class Stage<T> {

    /** Where every normalizer of a source starts: the bytes the source fetched. */
    public static final Stage<byte[]> RAW = new Stage<>("raw") {};

    /** Where every normalizer ends: the records the crawler writes to the inbox. */
    public static final Stage<Stream<byte[]>> SERIALIZED = new Stage<>("serialized") {};

    private final String name;
    private final String type;

    protected Stage(String name) {
        this.name = Objects.requireNonNull(name, "name");
        this.type = TypeNames.canonical(capturedType().getTypeName());
    }

    public String name() {
        return name;
    }

    /** The full type of the stage, spelled as {@link TypeNames#canonical(String)} spells it. */
    public String type() {
        return type;
    }

    private Type capturedType() {
        if (getClass().getGenericSuperclass() instanceof ParameterizedType stage
                && !(stage.getActualTypeArguments()[0] instanceof TypeVariable<?>))
            return stage.getActualTypeArguments()[0];
        throw new IllegalStateException("The stage '" + name + "' has no concrete type. Declare it as "
                + "new Stage<TheType>(\"" + name + "\") {}");
    }

    @Override
    public String toString() {
        return name + " (" + type + ")";
    }
}
