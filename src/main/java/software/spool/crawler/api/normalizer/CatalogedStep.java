package software.spool.crawler.api.normalizer;

import software.spool.core.pipeline.Step;

/**
 * A step of the catalog: its name, its class and the full types it takes and gives, as the annotation processor
 * wrote them when the class was compiled.
 */
public record CatalogedStep(String name, String className, String inputType, String outputType) {

    /** A new instance of the step, made with its constructor without arguments. */
    public NamedStep instantiate(ClassLoader classLoader) {
        try {
            Object step = Class.forName(className, true, classLoader).getDeclaredConstructor().newInstance();
            return new NamedStep(name, (Step<?, ?>) step, inputType, outputType);
        } catch (ReflectiveOperationException | ClassCastException | LinkageError e) {
            throw new NormalizerCompositionException("The normalizer step " + name + " could not be created from "
                    + className + ": " + e.getMessage(), e);
        }
    }
}
