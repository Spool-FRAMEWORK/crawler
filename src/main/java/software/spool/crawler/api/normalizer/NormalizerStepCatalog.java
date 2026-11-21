package software.spool.crawler.api.normalizer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;

/**
 * Every step annotated with {@link NormalizerStep} on the classpath, read from the catalogs the annotation
 * processor wrote when each jar was compiled. Nothing is instantiated until a step is asked for.
 */
public final class NormalizerStepCatalog {

    /** Where the annotation processor writes the catalog of each compiled module. */
    public static final String LOCATION = "META-INF/spool/normalizer-steps";

    /** The separator between the fields of a line of the catalog: name, class, input type, output type. */
    public static final String SEPARATOR = "\t";

    private final Map<String, CatalogedStep> steps;
    private final ClassLoader classLoader;

    private NormalizerStepCatalog(Map<String, CatalogedStep> steps, ClassLoader classLoader) {
        this.steps = steps;
        this.classLoader = classLoader;
    }

    public static NormalizerStepCatalog load(ClassLoader classLoader) {
        Map<String, CatalogedStep> steps = new LinkedHashMap<>();
        try {
            for (URL catalog : Collections.list(classLoader.getResources(LOCATION)))
                read(catalog).forEach((name, step) -> add(steps, step));
        } catch (IOException e) {
            throw new UncheckedIOException("The catalogs of normalizer steps could not be read", e);
        }
        return new NormalizerStepCatalog(steps, classLoader);
    }

    public Optional<CatalogedStep> find(String name) {
        return Optional.ofNullable(steps.get(name));
    }

    /** A new instance of the step called {@code name}, if the catalog has it. */
    public Optional<NamedStep> instantiate(String name) {
        return find(name).map(step -> step.instantiate(classLoader));
    }

    public Set<String> names() {
        return new TreeSet<>(steps.keySet());
    }

    private static void add(Map<String, CatalogedStep> steps, CatalogedStep step) {
        CatalogedStep existing = steps.putIfAbsent(step.name(), step);
        if (existing != null && !existing.className().equals(step.className()))
            throw new NormalizerCompositionException("The normalizer step " + step.name() + " is declared twice: by "
                    + existing.className() + " and by " + step.className());
    }

    private static Map<String, CatalogedStep> read(URL catalog) throws IOException {
        Map<String, CatalogedStep> steps = new LinkedHashMap<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(catalog.openStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) continue;
                String[] fields = line.split(SEPARATOR, -1);
                if (fields.length != 4)
                    throw new NormalizerCompositionException("The catalog " + catalog + " has a line that is not "
                            + "name, class, input type and output type: " + line);
                steps.put(fields[0], new CatalogedStep(fields[0], fields[1],
                        TypeNames.canonical(fields[2]), TypeNames.canonical(fields[3])));
            }
        }
        return steps;
    }
}
