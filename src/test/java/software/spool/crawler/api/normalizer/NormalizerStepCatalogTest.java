package software.spool.crawler.api.normalizer;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import software.spool.core.pipeline.Step;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class NormalizerStepCatalogTest {

    @TempDir
    Path firstJar;

    @TempDir
    Path secondJar;

    public static class Trim implements Step<String, String> {
        @Override
        public String apply(String input) {
            return input.trim();
        }
    }

    private static void writeCatalog(Path jar, String... lines) throws IOException {
        Path catalog = jar.resolve(NormalizerStepCatalog.LOCATION);
        Files.createDirectories(catalog.getParent());
        Files.write(catalog, java.util.List.of(lines));
    }

    private NormalizerStepCatalog catalogOf(Path... jars) throws IOException {
        URL[] urls = new URL[jars.length];
        for (int index = 0; index < jars.length; index++) urls[index] = jars[index].toUri().toURL();
        return NormalizerStepCatalog.load(new URLClassLoader(urls, getClass().getClassLoader()));
    }

    @Test
    void joinsTheCatalogsOfEveryJar() throws IOException {
        writeCatalog(firstJar, "TRIM\t" + Trim.class.getName() + "\tjava.lang.String\tjava.lang.String");
        writeCatalog(secondJar, "OTHER\tsteps.Other\tbyte[]\tjava.lang.String");

        NormalizerStepCatalog catalog = catalogOf(firstJar, secondJar);

        assertThat(catalog.names()).containsExactly("OTHER", "TRIM");
        assertThat(catalog.find("OTHER")).contains(new CatalogedStep("OTHER", "steps.Other", "byte[]", "java.lang.String"));
    }

    @Test
    void createsAStepWithItsTypes() throws Exception {
        writeCatalog(firstJar, "TRIM\t" + Trim.class.getName() + "\tjava.lang.String\tjava.lang.String");

        NamedStep trim = catalogOf(firstJar).instantiate("TRIM").orElseThrow();

        assertThat(trim.inputType()).isEqualTo("java.lang.String");
        assertThat(((Trim) trim.step()).apply("  ana ")).isEqualTo("ana");
    }

    @Test
    void findsNothingForANameNobodyDeclared() throws IOException {
        assertThat(catalogOf(firstJar).find("MISSING")).isEmpty();
    }

    @Test
    void refusesTheSameNameInTwoJars() throws IOException {
        writeCatalog(firstJar, "TRIM\tsteps.First\tjava.lang.String\tjava.lang.String");
        writeCatalog(secondJar, "TRIM\tsteps.Second\tjava.lang.String\tjava.lang.String");

        assertThatThrownBy(() -> catalogOf(firstJar, secondJar))
                .isInstanceOf(NormalizerCompositionException.class)
                .hasMessage("The normalizer step TRIM is declared twice: by steps.First and by steps.Second");
    }

    @Test
    void saysWhichClassCouldNotBeCreated() throws IOException {
        writeCatalog(firstJar, "GONE\tsteps.Gone\tjava.lang.String\tjava.lang.String");

        assertThatThrownBy(() -> catalogOf(firstJar).instantiate("GONE"))
                .isInstanceOf(NormalizerCompositionException.class)
                .hasMessageStartingWith("The normalizer step GONE could not be created from steps.Gone");
    }
}
