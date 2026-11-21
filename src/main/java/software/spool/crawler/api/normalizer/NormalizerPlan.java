package software.spool.crawler.api.normalizer;

import software.spool.core.pipeline.ObservedStep;
import software.spool.core.pipeline.Pipeline;
import software.spool.core.pipeline.Step;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * A normalizer before it is built: the stages it publishes and the steps between them, in order. Steps are
 * added after a stage or replace the ones between two stages, and every change is checked against the types of
 * the stages around it, so a plan that builds always fits. A plan never changes: every operation gives a new one.
 *
 * <pre>{@code
 * NormalizerPlan.from(Stage.RAW)
 *         .then(PARSED, "parse", new ParseSummary())
 *         .then(Stage.SERIALIZED, "serialize", new ToJsonLines())
 *         .build();
 * }</pre>
 *
 * @param <I> what the normalizer takes
 * @param <O> what the last stage of the plan holds so far
 */
public final class NormalizerPlan<I, O> {

    private final Stage<I> start;
    private final List<Link> links;

    private NormalizerPlan(Stage<I> start, List<Link> links) {
        this.start = start;
        this.links = List.copyOf(links);
    }

    public static <T> NormalizerPlan<T, T> from(Stage<T> start) {
        return new NormalizerPlan<>(start, List.of());
    }

    /** Adds a step at the end, which gives the new stage {@code stage}. */
    public <K> NormalizerPlan<I, K> then(Stage<K> stage, String stepName, Step<O, K> step) {
        if (stages().contains(stage.name()))
            throw new NormalizerCompositionException("The stage '" + stage.name() + "' is already in the normalizer");
        NamedStep named = new NamedStep(stepName, step, typeAt(lastIndex()), stage.type());
        return new NormalizerPlan<>(start, inserted(links.size(), List.of(new Link(named, Optional.of(stage.name())))));
    }

    /** Adds a step at the end, without a stage of its own, checking that it takes what the plan gives so far. */
    public NormalizerPlan<I, ?> then(NamedStep step) {
        String given = typeAt(lastIndex());
        if (!step.inputType().equals(given))
            throw new NormalizerCompositionException(step.name() + " takes " + step.inputType() + ", but "
                    + (links.isEmpty() ? "the normalizer starts at '" + start.name() + "' with " : "the step before it gives ")
                    + given);
        return new NormalizerPlan<I, Object>(start, inserted(links.size(), List.of(new Link(step, Optional.empty()))));
    }

    public <T> NormalizerPlan<I, O> after(Stage<T> stage, String stepName, Step<T, T> step) {
        return after(stage.name(), List.of(NamedStep.of(stepName, stage, stage, step)));
    }

    /**
     * Adds {@code steps} after the stage, in order. Each one takes and gives what the stage holds, so what comes
     * after does not notice them. Steps added after the same stage before run first.
     */
    public NormalizerPlan<I, O> after(String stage, List<NamedStep> steps) {
        int index = indexOf(stage);
        String type = typeAt(index);
        for (NamedStep step : steps)
            if (!step.inputType().equals(type) || !step.outputType().equals(type))
                throw new NormalizerCompositionException(step.name() + " takes " + step.inputType() + " and gives "
                        + step.outputType() + ", but a step after '" + stage + "' has to take and give " + type);
        List<Link> added = steps.stream().map(step -> new Link(step, Optional.of(stage))).toList();
        return new NormalizerPlan<>(start, inserted(index + 1, added));
    }

    public <A, B> NormalizerPlan<I, O> replace(Stage<A> from, Stage<B> to, String stepName, Step<A, B> step) {
        return replace(from.name(), to.name(), List.of(NamedStep.of(stepName, from, to, step)));
    }

    /** Replaces the step that gives {@code stage}. The steps after it still run. */
    public NormalizerPlan<I, O> replace(String stage, NamedStep step) {
        int index = indexOf(stage);
        if (index < 0)
            throw new NormalizerCompositionException("No step gives the stage '" + stage
                    + "': it is where the normalizer starts");
        return replaced(index - 1, index, List.of(step));
    }

    /**
     * Replaces every step from the stage {@code from} to the stage {@code to} with {@code steps}. Inside the chain
     * the types may change; the first step takes what {@code from} holds and the last gives what {@code to} holds.
     */
    public NormalizerPlan<I, O> replace(String from, String to, List<NamedStep> steps) {
        int fromIndex = indexOf(from);
        int toIndex = indexOf(to);
        if (toIndex <= fromIndex)
            throw new NormalizerCompositionException("The stage '" + to + "' does not come after '" + from + "'");
        return replaced(fromIndex, toIndex, steps);
    }

    /** The names of the stages, in the order they are reached. */
    public List<String> stages() {
        return Stream.concat(Stream.of(start.name()), links.stream().flatMap(link -> link.stage().stream()))
                .distinct()
                .toList();
    }

    /** The plan in one line, with every step and the stages between them: what the normalizer really runs. */
    public String describe() {
        return Stream.concat(Stream.of("[" + start.name() + "]"),
                        links.stream().map(link -> link.step().name() + link.stage().map(stage -> " [" + stage + "]").orElse("")))
                .collect(Collectors.joining(" "));
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public Normalizer<I> build() {
        String last = typeAt(lastIndex());
        if (!last.equals(Stage.SERIALIZED.type()))
            throw new NormalizerCompositionException("The normalizer ends with " + last + ", but it has to end with "
                    + Stage.SERIALIZED.type() + ", the records the crawler writes");
        Pipeline pipeline = Pipeline.start();
        for (Link link : links)
            pipeline = pipeline.add(new ObservedStep<>(link.step().name(), (Step) link.step().step()));
        return new Normalizer<>((Pipeline<I, Stream<byte[]>>) pipeline);
    }

    private NormalizerPlan<I, O> replaced(int fromIndex, int toIndex, List<NamedStep> steps) {
        if (steps.isEmpty())
            throw new NormalizerCompositionException("A replacement needs at least one step");
        String given = typeAt(fromIndex);
        Optional<NamedStep> previous = Optional.empty();
        for (NamedStep step : steps) {
            if (!step.inputType().equals(given))
                throw new NormalizerCompositionException(step.name() + " takes " + step.inputType() + ", but "
                        + previous.map(before -> before.name() + ", which comes before, gives ")
                                  .orElse("the stage '" + labelOf(fromIndex) + "' holds ")
                        + given);
            given = step.outputType();
            previous = Optional.of(step);
        }
        String expected = typeAt(toIndex);
        if (!given.equals(expected))
            throw new NormalizerCompositionException(previous.orElseThrow().name() + " gives " + given
                    + ", but the stage '" + labelOf(toIndex) + "' holds " + expected);
        List<Link> chain = new ArrayList<>();
        for (int position = 0; position < steps.size(); position++)
            chain.add(new Link(steps.get(position),
                    position == steps.size() - 1 ? links.get(toIndex).stage() : Optional.empty()));
        List<Link> result = new ArrayList<>(links.subList(0, fromIndex + 1));
        result.addAll(chain);
        result.addAll(links.subList(toIndex + 1, links.size()));
        return new NormalizerPlan<>(start, result);
    }

    /** Where the stage is reached last, {@code -1} for the start; steps added after it go after that point. */
    private int indexOf(String stage) {
        for (int index = links.size() - 1; index >= 0; index--)
            if (links.get(index).stage().filter(stage::equals).isPresent()) return index;
        if (start.name().equals(stage)) return -1;
        throw new NormalizerCompositionException("The normalizer has no stage '" + stage + "'. Its stages are: "
                + String.join(", ", stages()));
    }

    private String typeAt(int index) {
        return index < 0 ? start.type() : links.get(index).step().outputType();
    }

    private String labelOf(int index) {
        return index < 0 ? start.name() : links.get(index).stage().orElse(links.get(index).step().name());
    }

    private int lastIndex() {
        return links.size() - 1;
    }

    private List<Link> inserted(int position, List<Link> added) {
        List<Link> result = new ArrayList<>(links);
        result.addAll(position, added);
        return result;
    }

    private record Link(NamedStep step, Optional<String> stage) {}
}
