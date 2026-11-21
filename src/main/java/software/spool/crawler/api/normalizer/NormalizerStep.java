package software.spool.crawler.api.normalizer;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Names a step so that a descriptor can use it in a normalizer. The annotated class implements
 * {@code Step<I, O>} with concrete types and has a public constructor without arguments; the annotation
 * processor checks both when compiling and writes the step to the catalog the descriptor is checked against.
 *
 * <p>The annotation does not say where the step goes: whoever composes the normalizer decides that.</p>
 */
@Documented
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.TYPE)
public @interface NormalizerStep {

    /** The name the descriptor uses, unique among every step on the classpath. */
    String value();
}
