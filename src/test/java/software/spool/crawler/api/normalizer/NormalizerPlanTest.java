package software.spool.crawler.api.normalizer;

import org.junit.jupiter.api.Test;
import software.spool.core.pipeline.Step;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class NormalizerPlanTest {

    private static final Stage<String> TEXT = new Stage<>("text") {};
    private static final Stage<Stream<String>> WORDS = new Stage<>("words") {};
    private static final Stage<String> OTHER_TEXT = new Stage<>("other") {};

    private static final Step<byte[], String> DECODE = bytes -> new String(bytes, StandardCharsets.UTF_8);
    private static final Step<String, Stream<String>> SPLIT_ON_SPACES = text -> Arrays.stream(text.split(" "));
    private static final Step<Stream<String>, Stream<byte[]>> ENCODE = words -> words.map(word -> word.getBytes(StandardCharsets.UTF_8));

    private static NormalizerPlan<byte[], Stream<byte[]>> plan() {
        return NormalizerPlan.from(Stage.RAW)
                .then(TEXT, "decode", DECODE)
                .then(WORDS, "split", SPLIT_ON_SPACES)
                .then(Stage.SERIALIZED, "encode", ENCODE);
    }

    private static List<String> recordsOf(NormalizerPlan<byte[], ?> plan, String payload) {
        return plan.build().normalize(payload.getBytes(StandardCharsets.UTF_8))
                .map(bytes -> new String(bytes, StandardCharsets.UTF_8))
                .toList();
    }

    private static NamedStep step(String name, String input, String output, Step<?, ?> step) {
        return new NamedStep(name, step, input, output);
    }

    @Test
    void runsItsStepsInOrder() {
        assertThat(recordsOf(plan(), "ana luis")).containsExactly("ana", "luis");
    }

    @Test
    void runsAStepAddedAfterAStage() {
        Step<String, String> upperCase = String::toUpperCase;

        assertThat(recordsOf(plan().after(TEXT, "upper-case", upperCase), "ana luis")).containsExactly("ANA", "LUIS");
    }

    @Test
    void runsStepsAddedAfterTheSameStageInTheOrderTheyWereAdded() {
        Step<String, String> upperCase = String::toUpperCase;
        Step<String, String> greet = text -> "hello-" + text;

        NormalizerPlan<byte[], Stream<byte[]>> extended = plan().after(TEXT, "upper-case", upperCase).after(TEXT, "greet", greet);

        assertThat(recordsOf(extended, "ana")).containsExactly("hello-ANA");
    }

    @Test
    void refusesAStepAfterAStageThatDoesNotGiveBackWhatItTakes() {
        NamedStep length = step("LENGTH", "java.lang.String", "java.lang.Integer", (Step<String, Integer>) String::length);

        assertThatThrownBy(() -> plan().after("text", List.of(length)))
                .isInstanceOf(NormalizerCompositionException.class)
                .hasMessageContaining("LENGTH")
                .hasMessageContaining("java.lang.Integer")
                .hasMessageContaining("has to take and give java.lang.String");
    }

    @Test
    void keepsTheStepsAfterAReplacedStep() {
        Step<String, Stream<String>> splitOnCommas = text -> Arrays.stream(text.split(","));

        NormalizerPlan<byte[], Stream<byte[]>> replaced = plan().replace(TEXT, WORDS, "split-on-commas", splitOnCommas);

        assertThat(recordsOf(replaced, "ana,luis")).containsExactly("ana", "luis");
    }

    @Test
    void replacesAStepByName() {
        NamedStep splitOnCommas = step("SPLIT_ON_COMMAS", "java.lang.String", "java.util.stream.Stream<java.lang.String>",
                (Step<String, Stream<String>>) text -> Arrays.stream(text.split(",")));

        assertThat(recordsOf(plan().replace("words", splitOnCommas), "ana,luis")).containsExactly("ana", "luis");
    }

    @Test
    void replacesAStretchWithAChainThatChangesTypeInside() {
        NamedStep length = step("LENGTH", "java.lang.String", "java.lang.Integer", (Step<String, Integer>) String::length);
        NamedStep encode = step("ENCODE_NUMBER", "java.lang.Integer", "java.util.stream.Stream<byte[]>",
                (Step<Integer, Stream<byte[]>>) number -> Stream.of(String.valueOf(number).getBytes(StandardCharsets.UTF_8)));

        NormalizerPlan<byte[], Stream<byte[]>> replaced = plan().replace("text", "serialized", List.of(length, encode));

        assertThat(recordsOf(replaced, "ana luis")).containsExactly("8");
        assertThat(replaced.describe()).isEqualTo("[raw] decode [text] LENGTH ENCODE_NUMBER [serialized]");
    }

    @Test
    void refusesAChainWhoseStepsDoNotFitEachOther() {
        NamedStep length = step("LENGTH", "java.lang.String", "java.lang.Integer", (Step<String, Integer>) String::length);
        NamedStep encodeWords = step("ENCODE_WORDS", "java.util.stream.Stream<java.lang.String>", "java.util.stream.Stream<byte[]>", ENCODE);

        assertThatThrownBy(() -> plan().replace("text", "serialized", List.of(length, encodeWords)))
                .hasMessage("ENCODE_WORDS takes java.util.stream.Stream<java.lang.String>, but LENGTH, which comes before, "
                        + "gives java.lang.Integer");
    }

    @Test
    void refusesAReplacementThatDoesNotGiveWhatTheStageHolds() {
        NamedStep words = step("WORDS", "java.lang.String", "java.util.stream.Stream<java.lang.String>", SPLIT_ON_SPACES);

        assertThatThrownBy(() -> plan().replace("serialized", words))
                .hasMessageContaining("WORDS takes java.lang.String, but the stage 'words' holds java.util.stream.Stream<java.lang.String>");
        assertThatThrownBy(() -> plan().replace("text", "serialized", List.of(words)))
                .hasMessage("WORDS gives java.util.stream.Stream<java.lang.String>, but the stage 'serialized' holds "
                        + "java.util.stream.Stream<byte[]>");
    }

    @Test
    void refusesAStageItDoesNotHaveAndNamesTheOnesItHas() {
        assertThatThrownBy(() -> plan().after("parsed", List.of()))
                .hasMessage("The normalizer has no stage 'parsed'. Its stages are: raw, text, words, serialized");
    }

    @Test
    void refusesAStageOfTheSameNameButAnotherType() {
        Step<String, String> same = text -> text;
        NormalizerPlan<byte[], Stream<byte[]>> plan = NormalizerPlan.from(Stage.RAW)
                .then(OTHER_TEXT, "decode", DECODE)
                .then(WORDS, "split", SPLIT_ON_SPACES)
                .then(Stage.SERIALIZED, "encode", ENCODE);

        assertThatThrownBy(() -> plan.after(TEXT, "same", same)).hasMessageContaining("no stage 'text'");
    }

    @Test
    void refusesToReplaceWhereItStarts() {
        assertThatThrownBy(() -> plan().replace("raw", step("X", "byte[]", "byte[]", (Step<byte[], byte[]>) bytes -> bytes)))
                .hasMessage("No step gives the stage 'raw': it is where the normalizer starts");
    }

    @Test
    void refusesAStretchThatGoesBackwards() {
        assertThatThrownBy(() -> plan().replace("words", "text", List.of()))
                .hasMessage("The stage 'text' does not come after 'words'");
    }

    @Test
    void buildsFromScratchWithNamedSteps() {
        NormalizerPlan<byte[], ?> plan = NormalizerPlan.from(Stage.RAW)
                .then(step("DECODE", "byte[]", "java.lang.String", DECODE))
                .then(step("SPLIT", "java.lang.String", "java.util.stream.Stream<java.lang.String>", SPLIT_ON_SPACES))
                .then(step("ENCODE", "java.util.stream.Stream<java.lang.String>", "java.util.stream.Stream<byte[]>", ENCODE));

        assertThat(recordsOf(plan, "ana luis")).containsExactly("ana", "luis");
        assertThat(plan.describe()).isEqualTo("[raw] DECODE SPLIT ENCODE");
    }

    @Test
    void refusesANamedStepThatDoesNotTakeWhatComesBefore() {
        assertThatThrownBy(() -> NormalizerPlan.from(Stage.RAW)
                .then(step("SPLIT", "java.lang.String", "java.util.stream.Stream<java.lang.String>", SPLIT_ON_SPACES)))
                .hasMessage("SPLIT takes java.lang.String, but the normalizer starts at 'raw' with byte[]");
    }

    @Test
    void refusesToBuildWhenItDoesNotEndInRecords() {
        NormalizerPlan<byte[], String> unfinished = NormalizerPlan.from(Stage.RAW).then(TEXT, "decode", DECODE);

        assertThatThrownBy(unfinished::build)
                .hasMessage("The normalizer ends with java.lang.String, but it has to end with "
                        + "java.util.stream.Stream<byte[]>, the records the crawler writes");
    }

    @Test
    void refusesTheSameStageTwice() {
        assertThatThrownBy(() -> NormalizerPlan.from(Stage.RAW).then(TEXT, "decode", DECODE).then(TEXT, "again", text -> text))
                .hasMessage("The stage 'text' is already in the normalizer");
    }

    @Test
    void describesEveryStepAndStage() {
        Step<String, String> upperCase = String::toUpperCase;

        assertThat(plan().after(TEXT, "upper-case", upperCase).describe())
                .isEqualTo("[raw] decode [text] upper-case [text] split [words] encode [serialized]");
        assertThat(plan().stages()).containsExactly("raw", "text", "words", "serialized");
    }

    @Test
    void writesTheTypeOfAStageInFull() {
        assertThat(WORDS.type()).isEqualTo("java.util.stream.Stream<java.lang.String>");
        assertThat(StandardNormalizers.EXTRACTED.type()).isEqualTo(
                "software.spool.crawler.api.normalizer.steps.PayloadContext<com.fasterxml.jackson.databind.JsonNode,"
                        + "com.fasterxml.jackson.databind.JsonNode>");
    }

    @Test
    void refusesAStageWithoutAConcreteType() {
        assertThatThrownBy(NormalizerPlanTest::stageOfAnyType)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("The stage 'any' has no concrete type");
    }

    private static <T> Stage<T> stageOfAnyType() {
        return new Stage<T>("any") {};
    }
}
