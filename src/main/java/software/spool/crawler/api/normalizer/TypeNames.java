package software.spool.crawler.api.normalizer;

/**
 * The single spelling of a Java type that stages and cataloged steps are compared by. Reflection and the compiler
 * write the same type differently ({@code Map<K, V>} against {@code Map<K,V>}, {@code Outer$Inner} against
 * {@code Outer.Inner}), and both have to land on the same text.
 */
public final class TypeNames {

    private TypeNames() {}

    public static String canonical(String typeName) {
        return typeName.replace(" ", "").replace('$', '.');
    }
}
