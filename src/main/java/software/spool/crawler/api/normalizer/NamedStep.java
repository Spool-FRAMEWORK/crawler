package software.spool.crawler.api.normalizer;

import software.spool.core.pipeline.Step;

import java.util.Objects;

/**
 * A step with its name and the full types it takes and gives, which is what a {@link NormalizerPlan} needs to
 * check a composition that the compiler could not, like one read from a descriptor.
 */
public record NamedStep(String name, Step<?, ?> step, String inputType, String outputType) {

    public NamedStep {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(step, "step");
        inputType = TypeNames.canonical(inputType);
        outputType = TypeNames.canonical(outputType);
    }

    public static <I, O> NamedStep of(String name, Stage<I> input, Stage<O> output, Step<I, O> step) {
        return new NamedStep(name, step, input.type(), output.type());
    }
}
