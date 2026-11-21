package software.spool.crawler.processor;

import software.spool.crawler.api.normalizer.NormalizerStep;
import software.spool.crawler.api.normalizer.NormalizerStepCatalog;
import software.spool.crawler.api.normalizer.TypeNames;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.NestingKind;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import javax.tools.Diagnostic;
import javax.tools.FileObject;
import javax.tools.StandardLocation;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Checks every class annotated with {@link NormalizerStep} while it compiles and writes the catalog of the module,
 * so that a descriptor naming a step can be checked against its types before anything runs. A step has to:
 *
 * <ul>
 *   <li>be a public, concrete class, top level or static nested;</li>
 *   <li>implement {@code Step<I, O>} with concrete types, generics included;</li>
 *   <li>have a public constructor without arguments;</li>
 *   <li>have a name no other step of the module has.</li>
 * </ul>
 */
@SupportedAnnotationTypes("software.spool.crawler.api.normalizer.NormalizerStep")
public final class NormalizerStepProcessor extends AbstractProcessor {

    private static final String STEP = "software.spool.core.pipeline.Step";

    private final Map<String, String> lines = new LinkedHashMap<>();
    private final Map<String, String> classesByName = new LinkedHashMap<>();

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment round) {
        for (Element element : round.getElementsAnnotatedWith(NormalizerStep.class))
            check(element);
        if (round.processingOver() && !lines.isEmpty())
            write();
        return false;
    }

    private void check(Element element) {
        String name = element.getAnnotation(NormalizerStep.class).value();
        if (element.getKind() != ElementKind.CLASS) {
            error(element, "@NormalizerStep(\"" + name + "\") goes on a class that implements Step");
            return;
        }
        TypeElement type = (TypeElement) element;
        String className = processingEnv.getElementUtils().getBinaryName(type).toString();
        if (name.isBlank()) {
            error(type, className + " has an empty @NormalizerStep name");
            return;
        }
        if (!isInstantiable(type)) {
            error(type, "The normalizer step " + name + " has to be a public, concrete class, top level or static "
                    + "nested, with a public constructor without arguments: " + className + " is not");
            return;
        }
        Optional<DeclaredType> step = stepOf(type.asType());
        if (step.isEmpty()) {
            error(type, "The normalizer step " + name + " has to implement " + STEP + ": " + className + " does not");
            return;
        }
        List<? extends TypeMirror> types = step.get().getTypeArguments();
        if (types.size() != 2) {
            error(type, "The normalizer step " + name + " implements Step without its types. Name them: Step<I, O>");
            return;
        }
        for (TypeMirror stepType : types)
            if (!isConcrete(stepType)) {
                error(type, "The normalizer step " + name + " implements " + step.get() + ", but " + stepType
                        + " is not a concrete type. Name the types in full");
                return;
            }
        String previous = classesByName.putIfAbsent(name, className);
        if (previous != null) {
            error(type, "The normalizer step name " + name + " is already used by " + previous);
            return;
        }
        lines.put(name, String.join(NormalizerStepCatalog.SEPARATOR, name, className,
                TypeNames.canonical(types.get(0).toString()), TypeNames.canonical(types.get(1).toString())));
    }

    private boolean isInstantiable(TypeElement type) {
        Set<Modifier> modifiers = type.getModifiers();
        boolean reachable = modifiers.contains(Modifier.PUBLIC) && !modifiers.contains(Modifier.ABSTRACT)
                && (type.getNestingKind() == NestingKind.TOP_LEVEL
                    || type.getNestingKind() == NestingKind.MEMBER && modifiers.contains(Modifier.STATIC));
        return reachable && ElementFilter.constructorsIn(type.getEnclosedElements()).stream()
                .anyMatch(constructor -> constructor.getModifiers().contains(Modifier.PUBLIC)
                        && constructor.getParameters().isEmpty());
    }

    /** The {@code Step<I, O>} the type implements, with its type arguments as the type sees them. */
    private Optional<DeclaredType> stepOf(TypeMirror type) {
        Deque<TypeMirror> pending = new ArrayDeque<>(List.of(type));
        while (!pending.isEmpty()) {
            TypeMirror current = pending.pop();
            if (current instanceof DeclaredType declared
                    && ((TypeElement) declared.asElement()).getQualifiedName().contentEquals(STEP))
                return Optional.of(declared);
            pending.addAll(processingEnv.getTypeUtils().directSupertypes(current));
        }
        return Optional.empty();
    }

    private static boolean isConcrete(TypeMirror type) {
        if (type.getKind() == TypeKind.TYPEVAR || type.getKind() == TypeKind.WILDCARD) return false;
        if (type instanceof ArrayType array) return isConcrete(array.getComponentType());
        if (type instanceof DeclaredType declared)
            return declared.getTypeArguments().stream().allMatch(NormalizerStepProcessor::isConcrete);
        return type.getKind().isPrimitive();
    }

    private void write() {
        try {
            FileObject catalog = processingEnv.getFiler()
                    .createResource(StandardLocation.CLASS_OUTPUT, "", NormalizerStepCatalog.LOCATION);
            try (Writer writer = new OutputStreamWriter(catalog.openOutputStream(), StandardCharsets.UTF_8)) {
                for (String line : lines.values()) writer.write(line + "\n");
            }
        } catch (IOException e) {
            processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR,
                    "The catalog of normalizer steps could not be written: " + e.getMessage());
        }
    }

    private void error(Element element, String message) {
        processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, message, element);
    }
}
