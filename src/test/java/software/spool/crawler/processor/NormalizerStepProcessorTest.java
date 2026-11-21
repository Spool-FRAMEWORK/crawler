package software.spool.crawler.processor;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import software.spool.crawler.api.normalizer.NormalizerStepCatalog;

import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.ToolProvider;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class NormalizerStepProcessorTest {

    @TempDir
    Path output;

    private record Compilation(boolean succeeded, List<String> errors, Path output) {
        List<String> catalog() throws IOException {
            return Files.readAllLines(output.resolve(NormalizerStepCatalog.LOCATION));
        }
    }

    private Compilation compile(Map<String, String> sources) {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
        List<JavaFileObject> files = sources.entrySet().stream()
                .map(source -> (JavaFileObject) new Source(source.getKey(), source.getValue()))
                .toList();
        JavaCompiler.CompilationTask task = compiler.getTask(null, null, diagnostics,
                List.of("-d", output.toString(), "-classpath", System.getProperty("java.class.path"), "-proc:only"),
                null, files);
        task.setProcessors(List.of(new NormalizerStepProcessor()));
        boolean succeeded = task.call();
        List<String> errors = diagnostics.getDiagnostics().stream()
                .filter(diagnostic -> diagnostic.getKind() == Diagnostic.Kind.ERROR)
                .map(diagnostic -> diagnostic.getMessage(null))
                .toList();
        return new Compilation(succeeded, errors, output);
    }

    @Test
    void writesEveryStepWithItsClassAndItsFullTypes() throws IOException {
        Compilation compilation = compile(Map.of(
                "steps.UpperCaseNames", """
                        package steps;
                        import com.fasterxml.jackson.databind.JsonNode;
                        @software.spool.crawler.api.normalizer.NormalizerStep("UPPERCASE_NAMES")
                        public class UpperCaseNames implements software.spool.core.pipeline.Step<JsonNode, JsonNode> {
                            public JsonNode apply(JsonNode input) { return input; }
                        }
                        """,
                "steps.Steps", """
                        package steps;
                        import java.util.stream.Stream;
                        import java.util.Map;
                        public class Steps {
                            @software.spool.crawler.api.normalizer.NormalizerStep("TEXT_TO_BYTES")
                            public static class TextToBytes implements software.spool.core.pipeline.Step<Stream<String>, Stream<byte[]>> {
                                public Stream<byte[]> apply(Stream<String> input) { return input.map(String::getBytes); }
                            }
                            @software.spool.crawler.api.normalizer.NormalizerStep("ROWS")
                            public static class Rows implements software.spool.core.pipeline.Step<byte[], Stream<Map<String, Object>>> {
                                public Stream<Map<String, Object>> apply(byte[] input) { return Stream.empty(); }
                            }
                        }
                        """));

        assertThat(compilation.errors()).isEmpty();
        assertThat(compilation.catalog()).containsExactlyInAnyOrder(
                "UPPERCASE_NAMES\tsteps.UpperCaseNames\tcom.fasterxml.jackson.databind.JsonNode\tcom.fasterxml.jackson.databind.JsonNode",
                "TEXT_TO_BYTES\tsteps.Steps$TextToBytes\tjava.util.stream.Stream<java.lang.String>\tjava.util.stream.Stream<byte[]>",
                "ROWS\tsteps.Steps$Rows\tbyte[]\tjava.util.stream.Stream<java.util.Map<java.lang.String,java.lang.Object>>");
    }

    @Test
    void readsTheTypesOfAStepThatInheritsThem() throws IOException {
        Compilation compilation = compile(Map.of(
                "steps.Identity", """
                        package steps;
                        public abstract class Identity<T> implements software.spool.core.pipeline.Step<T, T> {
                            public T apply(T input) { return input; }
                        }
                        """,
                "steps.SameText", """
                        package steps;
                        @software.spool.crawler.api.normalizer.NormalizerStep("SAME_TEXT")
                        public class SameText extends Identity<String> {}
                        """));

        assertThat(compilation.errors()).isEmpty();
        assertThat(compilation.catalog()).containsExactly("SAME_TEXT\tsteps.SameText\tjava.lang.String\tjava.lang.String");
    }

    @Test
    void refusesTwoStepsWithTheSameName() {
        Compilation compilation = compile(Map.of(
                "steps.First", """
                        package steps;
                        @software.spool.crawler.api.normalizer.NormalizerStep("SAME")
                        public class First implements software.spool.core.pipeline.Step<String, String> {
                            public String apply(String input) { return input; }
                        }
                        """,
                "steps.Second", """
                        package steps;
                        @software.spool.crawler.api.normalizer.NormalizerStep("SAME")
                        public class Second implements software.spool.core.pipeline.Step<String, String> {
                            public String apply(String input) { return input; }
                        }
                        """));

        assertThat(compilation.succeeded()).isFalse();
        assertThat(compilation.errors()).singleElement().asString()
                .startsWith("The normalizer step name SAME is already used by steps.");
    }

    @Test
    void refusesAStepWhoseTypesAreNotConcrete() {
        Compilation compilation = compile(Map.of(
                "steps.Anything", """
                        package steps;
                        @software.spool.crawler.api.normalizer.NormalizerStep("ANYTHING")
                        public class Anything<T> implements software.spool.core.pipeline.Step<T, java.util.List<?>> {
                            public java.util.List<?> apply(T input) { return null; }
                        }
                        """));

        assertThat(compilation.succeeded()).isFalse();
        assertThat(compilation.errors()).singleElement().asString()
                .contains("The normalizer step ANYTHING implements")
                .contains("is not a concrete type");
    }

    @Test
    void refusesAStepWithoutAConstructorWithoutArguments() {
        Compilation compilation = compile(Map.of(
                "steps.Configured", """
                        package steps;
                        @software.spool.crawler.api.normalizer.NormalizerStep("CONFIGURED")
                        public class Configured implements software.spool.core.pipeline.Step<String, String> {
                            public Configured(String prefix) {}
                            public String apply(String input) { return input; }
                        }
                        """));

        assertThat(compilation.errors()).singleElement().asString()
                .contains("The normalizer step CONFIGURED has to be a public, concrete class")
                .contains("steps.Configured is not");
    }

    @Test
    void refusesAnAnnotatedClassThatIsNotAStep() {
        Compilation compilation = compile(Map.of(
                "steps.NotAStep", """
                        package steps;
                        @software.spool.crawler.api.normalizer.NormalizerStep("NOT_A_STEP")
                        public class NotAStep {}
                        """));

        assertThat(compilation.errors()).containsExactly(
                "The normalizer step NOT_A_STEP has to implement software.spool.core.pipeline.Step: steps.NotAStep does not");
    }

    @Test
    void writesNoCatalogWhenNothingIsAnnotated() {
        Compilation compilation = compile(Map.of("steps.Plain", "package steps; public class Plain {}"));

        assertThat(compilation.succeeded()).isTrue();
        assertThat(compilation.output().resolve(NormalizerStepCatalog.LOCATION)).doesNotExist();
    }

    private static final class Source extends SimpleJavaFileObject {
        private final String code;

        Source(String className, String code) {
            super(URI.create("string:///" + className.replace('.', '/') + Kind.SOURCE.extension), Kind.SOURCE);
            this.code = code;
        }

        @Override
        public CharSequence getCharContent(boolean ignoreEncodingErrors) {
            return code;
        }
    }
}
