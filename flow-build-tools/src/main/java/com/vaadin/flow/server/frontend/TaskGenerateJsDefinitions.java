/*
 * Copyright 2000-2026 Vaadin Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.vaadin.flow.server.frontend;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.CodeSource;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.IntStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.databind.node.ArrayNode;

import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.DomEvent;
import com.vaadin.flow.component.EventData;
import com.vaadin.flow.component.internal.BundledEventData;
import com.vaadin.flow.internal.FrontendUtils;
import com.vaadin.flow.internal.JacksonUtils;
import com.vaadin.flow.internal.ReflectTools;
import com.vaadin.flow.js.JsCall;
import com.vaadin.flow.js.JsDefinition;
import com.vaadin.flow.js.JsDefinitionProxy;
import com.vaadin.flow.js.JsDefinitionProxy.Kind;
import com.vaadin.flow.js.JsExpression;
import com.vaadin.flow.js.JsInvokers;
import com.vaadin.flow.js.JsSnippet;
import com.vaadin.flow.js.JsSnippets;
import com.vaadin.flow.js.internal.JsWithExecutionContext;
import com.vaadin.flow.server.frontend.scanner.ClassFinder;

import static com.vaadin.flow.internal.FrontendUtils.GENERATED;
import static com.vaadin.flow.internal.FrontendUtils.JS_DEFINITIONS_FILE_NAME;

/**
 * Generates {@link FrontendUtils#JS_DEFINITIONS_FILE_NAME}, which registers the
 * JavaScript of every {@link JsDefinition} interface on the class path, one
 * that extends {@link JsInvokers} or {@link JsSnippets}, as an ordinary
 * function of the bundle: in one registry for the methods that run their
 * JavaScript when called, in another for those of them that are annotated with
 * {@link JsWithExecutionContext} and take the execution context as their first
 * parameter, and in a third for the methods that declare a {@link JsSnippet},
 * which the client turns into a function when it decodes the snippet. A
 * definition that does not pass
 * {@link JsDefinitionProxy#checkDefinition(Class)} is left out, with an error
 * in the log.
 * <p>
 * A production build that uses the bytecode scanner renders only the
 * definitions and event types that the scanner reaches from the entry points of
 * the application, together with those of Flow itself, the same way it collects
 * {@code @JsModule}. A class that is only used through reflection is reached by
 * naming it in {@code @Uses} on a class that is reached.
 * <p>
 * The snippet registry also holds the {@link EventData} values and the
 * {@link DomEvent#filter()} of the {@link DomEvent} classes on the class path
 * that the client would otherwise compile from a string, see
 * {@link BundledEventData}, so that the server can send the identifier of a
 * function instead of the value.
 * <p>
 * This is what lets the client run a server-initiated call without compiling
 * anything from a string: the server sends the identifier of the function to
 * run, which is a hash of the JavaScript, and the function is already in the
 * bundle. The call survives a content security policy that does not allow
 * <code>unsafe-eval</code>, the JavaScript an application can be made to run is
 * known when it is built, and what declared it in Java stays there.
 * <p>
 * Outside production mode the file also registers what a developer wrote for
 * each function, so that a message about a call in the browser names it rather
 * than a hash. A production bundle carries the functions alone.
 * <p>
 * Next to the file it writes the identifiers of the functions it registers,
 * which the stats of a bundle list, so that a build can tell whether a bundle
 * carries every function an application needs.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 * 
 * @since 25.4
 */
public class TaskGenerateJsDefinitions extends AbstractTaskClientGenerator {

    // Snippets are evaluated by what they are passed to, which may need their
    // value during an event dispatch, so they are kept apart from the async
    // functions a call runs
    private static final String SNIPPETS = "window.Vaadin.Flow.jsSnippets = window.Vaadin.Flow.jsSnippets || {};";

    // What the declared JavaScript of a function that gets the execution
    // context calls it by
    private static final String EXECUTION_CONTEXT = "$executionContext";

    // A function that gets the execution context is kept apart from the
    // others, so that being found here is what tells the client to pass it:
    // nothing in a message can ask for the context to be given to a function
    private static final String WITH_EXECUTION_CONTEXT = "window.Vaadin.Flow.jsDefinitionsWithExecutionContext = window.Vaadin.Flow.jsDefinitionsWithExecutionContext || {};";

    private static final List<String> HEADER = List.of("// @ts-nocheck",
            "window.Vaadin = window.Vaadin || {};",
            "window.Vaadin.Flow = window.Vaadin.Flow || {};",
            "window.Vaadin.Flow.jsDefinitions = window.Vaadin.Flow.jsDefinitions || {};",
            SNIPPETS, WITH_EXECUTION_CONTEXT);

    // What a message about a call names it by, which is of no use to a browser
    // running the application and is left out of a production bundle
    private static final String NAMES = "window.Vaadin.Flow.jsDefinitionNames = window.Vaadin.Flow.jsDefinitionNames || {};";

    // A name as nameOf renders it, with the interface it names in the group
    private static final Pattern DEFINITION_NAME = Pattern.compile(
            "^window\\.Vaadin\\.Flow\\.jsDefinitionNames\\[\"[^\"]*\"\\] = \"([^\"/]+)\\.[^.\"/]+/\\d+\";$",
            Pattern.MULTILINE);

    /**
     * The file next to the generated one that lists the identifiers of the
     * functions it registers, which vite.generated.ts copies into the stats of
     * a bundle. It lists what a browser can run and nothing of what declared
     * it, as the bundle does.
     */
    static final String FUNCTION_IDS_FILE_NAME = "vaadin-js-definition-ids.json";

    // Writing this file again while the application runs replaces it in the
    // browser that has it: everything above only writes into the registry, so
    // the module can accept its own update and nothing else has to be reloaded
    // for a changed declaration to take effect. The dev server drops the block
    // from a production build, where import.meta.hot is not defined.
    // The export is for https://github.com/vaadin/flow/issues/14184
    private static final List<String> FOOTER = List.of("if (import.meta.hot) {",
            "  import.meta.hot.accept();", "}", "export {};");

    private final Options options;

    TaskGenerateJsDefinitions(Options options) {
        this.options = options;
    }

    /**
     * The generated file and the identifiers of the functions it registers,
     * each with what declares its JavaScript.
     *
     * @param content
     *            the content of the generated file
     * @param functionIds
     *            the binary name of the class that declares the JavaScript of
     *            each function the file registers, by the identifier of the
     *            function, in the order of the file
     */
    record Rendered(String content, Map<String, String> functionIds) {
    }

    /**
     * One function the file registers: its identifier, the binary name of the
     * class that declares its JavaScript and what it is written as.
     */
    private record RenderedFunction(String id, String declaredBy, String text) {
    }

    @Override
    public void execute() throws ExecutionFailedException {
        Rendered rendered = render();
        File functionIdsFile = new File(getGeneratedFile().getParentFile(),
                FUNCTION_IDS_FILE_NAME);
        try {
            writeIfChanged(getGeneratedFile(), rendered.content());
            ArrayNode functionIds = JacksonUtils.createArrayNode();
            rendered.functionIds().keySet().forEach(functionIds::add);
            writeIfChanged(functionIdsFile,
                    JacksonUtils.toFileJson(functionIds));
        } catch (IOException exception) {
            throw new ExecutionFailedException("Error writing '"
                    + getGeneratedFile() + "' or '" + functionIdsFile + "'",
                    exception);
        }
    }

    @Override
    protected String getFileContent() {
        return render().content();
    }

    /**
     * Renders the file for the definitions and event types on the class path,
     * or, for an optimized production bundle, for those of them the application
     * can reach and those of Flow's own jar.
     *
     * @return the content of the file and the functions it registers
     */
    Rendered render() {
        // Generating the file is scanning for what goes into it, which a
        // caller that writes it again while the application runs does not do:
        // it passes the definitions in, through updateJsDefinitions
        ClassFinder classFinder = Objects.requireNonNull(
                options.getClassFinder(),
                "Generating the file needs a class finder to scan for the JavaScript definitions with");
        Collection<Class<?>> definitions = classFinder
                .getAnnotatedClasses(JsDefinition.class);
        Collection<Class<?>> eventTypes = findDomEventTypes(classFinder);
        if (options.isProductionMode() && options.isUseByteCodeScanner()) {
            // An optimized production bundle carries what the application can
            // reach, as it does for @JsModule, so that a library declaring
            // JavaScript the application does not use leaves the bundle as it
            // is. Development keeps everything: the dev server calls
            // definitions of its own that no entry point leads to, and a
            // hotswapped class may start using any definition.
            Predicate<Class<?>> needed = createNeededFilter(classFinder);
            definitions = definitions.stream().filter(needed).toList();
            eventTypes = eventTypes.stream().filter(needed).toList();
        }
        return render(definitions, eventTypes, !options.isProductionMode());
    }

    /**
     * What an optimized bundle carries the JavaScript of: the classes the
     * bytecode scanner reaches from the entry points of the application, and
     * every class of Flow's own jar. Flow calls some of its definitions from
     * request handlers that no entry point leads to, and its own JavaScript is
     * what the framework needs to work.
     */
    private Predicate<Class<?>> createNeededFilter(ClassFinder classFinder) {
        Set<String> reached = options.getFrontendDependenciesScanner()
                .getClasses();
        URL flowLocation;
        try {
            flowLocation = findLocation(
                    classFinder.loadClass(JsDefinition.class.getName()));
        } catch (ClassNotFoundException e) {
            // Nothing can declare a definition without the annotation
            flowLocation = null;
        }
        URL flow = flowLocation;
        return type -> reached.contains(type.getName())
                || Objects.equals(flow, findLocation(type));
    }

    private static URL findLocation(Class<?> type) {
        CodeSource codeSource = type.getProtectionDomain().getCodeSource();
        return codeSource == null ? null : codeSource.getLocation();
    }

    /**
     * Renders the file that registers the JavaScript of the given definition
     * interfaces.
     * <p>
     * Package private: what regenerating the file outside a build looks like is
     * {@link #updateJsDefinitions(Options, Collection)}, which goes through
     * this.
     *
     * @param definitions
     *            the JavaScript definitions to render, not <code>null</code>
     * @param withNames
     *            whether to register what a message about a call names it by,
     *            which is for a development bundle
     * @return the content of the generated file
     */
    static String renderFileContent(Collection<Class<?>> definitions,
            boolean withNames) {
        return renderFileContent(definitions, List.of(), withNames);
    }

    /**
     * Renders the file that registers the JavaScript of the given definition
     * interfaces and the {@link EventData} values of the given event types that
     * {@link BundledEventData} puts into the bundle.
     *
     * @param definitions
     *            the JavaScript definitions to render, not <code>null</code>
     * @param eventTypes
     *            the classes annotated with {@link DomEvent} to render the
     *            event data of, not <code>null</code>
     * @param withNames
     *            whether to register what a message about a call names it by,
     *            which is for a development bundle
     * @return the content of the generated file
     */
    static String renderFileContent(Collection<Class<?>> definitions,
            Collection<Class<?>> eventTypes, boolean withNames) {
        return render(definitions, eventTypes, withNames).content();
    }

    /**
     * Renders the file that registers the JavaScript of the given definition
     * interfaces and the {@link EventData} values of the given event types that
     * {@link BundledEventData} puts into the bundle, together with the
     * identifiers of the functions it registers.
     *
     * @param definitions
     *            the JavaScript definitions to render, not <code>null</code>
     * @param eventTypes
     *            the classes annotated with {@link DomEvent} to render the
     *            event data of, not <code>null</code>
     * @param withNames
     *            whether to register what a message about a call names it by,
     *            which is for a development bundle
     * @return the content of the file and the functions it registers
     */
    static Rendered render(Collection<Class<?>> definitions,
            Collection<Class<?>> eventTypes, boolean withNames) {
        List<RenderedFunction> functions = new ArrayList<>();
        definitions.stream().sorted(Comparator.comparing(Class::getName))
                .forEach(definition -> functions
                        .addAll(renderFunctions(definition, withNames)));
        functions.addAll(renderEventDataFunctions(eventTypes, withNames));

        List<String> lines = new ArrayList<>(renderHeader(withNames));
        Map<String, String> functionIds = new LinkedHashMap<>();
        for (RenderedFunction function : functions) {
            lines.addAll(Arrays
                    .asList(function.text().split(System.lineSeparator())));
            // The identifier is a hash of the JavaScript, so a function
            // declared twice is one function, which the first one names
            functionIds.putIfAbsent(function.id(), function.declaredBy());
        }
        lines.addAll(FOOTER);
        return new Rendered(String.join(System.lineSeparator(), lines),
                functionIds);
    }

    /**
     * Renders one snippet per distinct {@link EventData} value and
     * {@link DomEvent#filter()} of the given event types that
     * {@link BundledEventData} puts into the bundle, under the identifier the
     * server sends for it. The snippet captures nothing, and the function it
     * evaluates to is called with the event and the element, the same names the
     * client compiles such a value with.
     */
    private static List<RenderedFunction> renderEventDataFunctions(
            Collection<Class<?>> eventTypes, boolean withNames) {
        // By identifier, for an output that does not depend on the order of
        // the scan, each with the first event type that declares it
        Map<String, String> expressions = new TreeMap<>();
        Map<String, String> declaredBy = new HashMap<>();
        Map<String, String> eventTypeOf = new HashMap<>();
        eventTypes.stream().sorted(Comparator.comparing(Class::getName))
                .forEach(eventType -> {
                    findBundledEventData(eventType).forEach(eventData -> {
                        // A value mapped to an element shares the function
                        // of its expression, and the client maps what the
                        // function returns
                        String functionId = BundledEventData
                                .getFunctionId(eventData);
                        expressions.putIfAbsent(functionId,
                                BundledEventData.getExpression(eventData));
                        eventTypeOf.putIfAbsent(functionId,
                                eventType.getName());
                        declaredBy.putIfAbsent(functionId,
                                eventType.getName()
                                        + " @EventData(\"" + BundledEventData
                                                .getExpression(eventData)
                                        + "\")");
                    });
                    String filter = BundledEventData
                            .getBundledFilter(eventType);
                    if (filter != null) {
                        String functionId = BundledEventData
                                .getFunctionId(filter);
                        expressions.putIfAbsent(functionId, filter);
                        eventTypeOf.putIfAbsent(functionId,
                                eventType.getName());
                        declaredBy.putIfAbsent(functionId, eventType.getName()
                                + " @DomEvent(filter = \"" + filter + "\")");
                    }
                });

        List<RenderedFunction> functions = new ArrayList<>();
        expressions.forEach((functionId, expression) -> {
            List<String> lines = new ArrayList<>();
            String id = quote(functionId);
            lines.add(String.format(
                    "window.Vaadin.Flow.jsSnippets[%s] = function () {", id));
            lines.add("  return function (event, element) {");
            // On lines of their own, so that a value ending with a comment
            // does not comment out what closes it
            lines.add("    return (");
            lines.add(expression);
            lines.add("    );");
            lines.add("  };");
            lines.add("};");
            if (withNames) {
                lines.add(String.format(
                        "window.Vaadin.Flow.jsDefinitionNames[%s] = %s;", id,
                        quote(declaredBy.get(functionId))));
            }
            functions.add(new RenderedFunction(functionId,
                    eventTypeOf.get(functionId),
                    String.join(System.lineSeparator(), lines)));
        });
        return functions;
    }

    /**
     * The component event types that are listened to as DOM events. Found as
     * subtypes rather than by the annotation: {@link DomEvent} is inherited, so
     * a subclass that declares its own constructor is listened to through the
     * annotation of its superclass, and a class finder that reports only the
     * classes an annotation is written on would leave its event data out of the
     * bundle.
     */
    private static List<Class<?>> findDomEventTypes(ClassFinder classFinder) {
        return classFinder.getSubTypesOf(ComponentEvent.class).stream()
                .filter(type -> type.isAnnotationPresent(DomEvent.class))
                .<Class<?>> map(type -> type).toList();
    }

    @SuppressWarnings("unchecked")
    private static List<String> findBundledEventData(Class<?> eventType) {
        if (!ComponentEvent.class.isAssignableFrom(eventType)) {
            return List.of();
        }
        try {
            return BundledEventData.getBundledEventData(
                    (Class<? extends ComponentEvent<?>>) eventType);
        } catch (IllegalArgumentException e) {
            // Left out rather than failing the build, as for a definition.
            // Listening to the event fails at runtime with the same message.
            if (eventType.getDeclaredAnnotation(DomEvent.class) != null) {
                getLogger().warn(
                        "Leaving the event data of {} out of the bundle: {}",
                        eventType.getName(), e.getMessage());
            } else {
                // A subclass that inherits the annotation and is only fired
                // from the server, without a DOM event constructor, is common
                // and no mistake
                getLogger().debug(
                        "Leaving the event data of {} out of the bundle: {}",
                        eventType.getName(), e.getMessage());
            }
            return List.of();
        }
    }

    /**
     * The JavaScript definitions of the given ones whose JavaScript the
     * generated file does not carry, which is the JavaScript a browser can run
     * of them.
     *
     * @param options
     *            where the file is, not <code>null</code>
     * @param definitions
     *            the JavaScript definitions to look for, not <code>null</code>
     * @return those the file does not carry, empty when it carries all of them
     */
    public static List<Class<?>> findMissingFromGeneratedFile(Options options,
            Collection<Class<?>> definitions) {
        String generated = readGeneratedFile(options);
        return definitions.stream()
                .filter(definition -> !isInGeneratedFile(definition, generated))
                .toList();
    }

    /**
     * The names of the JavaScript definitions that the generated file names a
     * function of, for a caller that has to find which of them a change of
     * another class affects without scanning for them. Only a file written for
     * a development build names what declared its functions.
     *
     * @param options
     *            where the file is, not <code>null</code>
     * @return the binary names of the definitions, empty when there is no file
     *         or it names nothing
     */
    public static Set<String> findDefinitionNamesInGeneratedFile(
            Options options) {
        String generated = readGeneratedFile(options);
        Set<String> names = new LinkedHashSet<>();
        if (generated != null) {
            Matcher matcher = DEFINITION_NAME.matcher(generated);
            while (matcher.find()) {
                names.add(matcher.group(1));
            }
        }
        return names;
    }

    /**
     * Writes the generated file again so that it carries what the given
     * definitions declare, for a caller that has to update it while the
     * application runs rather than as part of a build.
     * <p>
     * What the file already holds is kept: a function it registers is what a
     * browser that has the file can run, and the caller only knows about the
     * definitions it passes in. A function nothing declares any longer stays in
     * the file with nothing calling it, until a build renders the file again.
     * <p>
     * Goes through the same write as {@link #execute()}, which leaves the file
     * alone when its content would not change and writes it atomically
     * otherwise, so a dev server is not told about an update that is not one
     * and never reads a file that is half written.
     *
     * @param options
     *            where the file is, not <code>null</code>
     * @param definitions
     *            the JavaScript definitions to write it for, not
     *            <code>null</code> and not empty
     * @return those of them the file does not carry afterwards, empty when it
     *         carries all of them
     */
    public static List<Class<?>> updateJsDefinitions(Options options,
            Collection<Class<?>> definitions) {
        String generated = readGeneratedFile(options);
        String content = withMissingEntries(generated, definitions);

        TaskGenerateJsDefinitions task = new TaskGenerateJsDefinitions(options);
        try {
            task.writeIfChanged(task.getGeneratedFile(), content);
        } catch (IOException e) {
            getLogger().debug("Could not write {}", task.getGeneratedFile(), e);
            // The file is as it was, so only what it was already missing is
            // missing now
            return definitions.stream().filter(
                    definition -> !isInGeneratedFile(definition, generated))
                    .toList();
        }
        // Everything asked for went into the content that was written
        return List.of();
    }

    /**
     * Whether the given content carries what the definition declares, compared
     * as this class renders it for a development build - the one the caller of
     * this runs in - so the JavaScript of every method, the number of arguments
     * it takes and its name have to match. A method that was removed does not
     * show up as a difference: its function stays in the file with nothing
     * calling it.
     */
    private static boolean isInGeneratedFile(Class<?> definition,
            String generated) {
        if (generated == null) {
            return false;
        }
        List<String> declared = renderDefinitionLines(definition, true);
        if (declared.isEmpty()) {
            // Declares no JavaScript, so there is nothing to carry
            return true;
        }
        return generated
                .contains(String.join(System.lineSeparator(), declared));
    }

    /**
     * The given content with the functions of the given definitions that it
     * does not hold yet put in front of what closes the file, or the whole file
     * rendered when there is nothing to add to.
     * <p>
     * Only the functions that are not in the content are added, so editing one
     * method of an interface does not write the others a second time. Nothing
     * the content holds is taken out of it: it is what a browser that has the
     * file can run, and this is asked for the definitions that changed rather
     * than for everything an application declares.
     */
    private static String withMissingEntries(String generated,
            Collection<Class<?>> definitions) {
        if (generated == null || generated.isBlank()) {
            return renderFileContent(definitions, true);
        }
        List<String> missing = definitions.stream()
                .sorted(Comparator.comparing(Class::getName))
                .flatMap(definition -> renderFunctions(definition, true)
                        .stream().map(RenderedFunction::text))
                .filter(function -> !generated.contains(function))
                .flatMap(function -> Arrays
                        .stream(function.split(System.lineSeparator())))
                .toList();
        if (missing.isEmpty()) {
            return generated;
        }
        String separator = System.lineSeparator();
        String footer = String.join(separator, FOOTER);
        // A file written before a registry was generated, such as the one of
        // snippets or of names, does not create it, and what is added may be
        // assigned into it
        List<String> added = new ArrayList<>(renderHeader(true).stream()
                .filter(line -> !generated.contains(line)).toList());
        added.addAll(missing);
        if (generated.contains(footer)) {
            return generated.replace(footer,
                    String.join(separator, added) + separator + footer);
        }
        // Written by another version of this class: what it holds is what a
        // browser has, so the functions go after it rather than instead of it.
        // The header only assigns what is not there, so repeating it is what
        // makes the content that follows land in the registry.
        return generated + separator
                + String.join(separator, renderHeader(true)) + separator
                + String.join(separator, missing) + separator + footer;
    }

    /**
     * What a generated file opens with: the registry a function is assigned
     * into, and the one a name is assigned into when names are rendered. Both
     * assign only what is not there, so a file can carry them more than once.
     */
    private static List<String> renderHeader(boolean withNames) {
        List<String> header = new ArrayList<>(HEADER);
        if (withNames) {
            header.add(NAMES);
        }
        return header;
    }

    private static String readGeneratedFile(Options options) {
        File generatedFile = new TaskGenerateJsDefinitions(options)
                .getGeneratedFile();
        if (!generatedFile.exists()) {
            return null;
        }
        try {
            return Files.readString(generatedFile.toPath(),
                    StandardCharsets.UTF_8);
        } catch (IOException e) {
            getLogger().debug("Could not read {}", generatedFile, e);
            return null;
        }
    }

    private static Logger getLogger() {
        return LoggerFactory.getLogger(TaskGenerateJsDefinitions.class);
    }

    /**
     * Renders what one JavaScript definition contributes to the generated file:
     * one function per method that declares JavaScript, registered under the
     * identifier of that function, which is what the server sends. The name of
     * the interface and of the method are not in it, so a browser is not told
     * what declared the JavaScript it runs.
     * <p>
     * Package private: whether a file carries what an interface declares is
     * answered by {@link #findMissingFromGeneratedFile(Options, Collection)},
     * which compares against this.
     *
     * @param definition
     *            the JavaScript definition to render, not <code>null</code>
     * @return the lines this definition contributes, empty if it declares no
     *         JavaScript
     */
    static List<String> renderDefinitionLines(Class<?> definition,
            boolean withNames) {
        return renderFunctions(definition, withNames).stream()
                .flatMap(function -> Arrays
                        .stream(function.text().split(System.lineSeparator())))
                .toList();
    }

    /**
     * What one JavaScript definition contributes, one registered function per
     * method that declares JavaScript, each as the lines it is written as.
     */
    private static List<RenderedFunction> renderFunctions(Class<?> definition,
            boolean withNames) {
        boolean snippets;
        try {
            snippets = JsDefinitionProxy
                    .checkDefinition(definition) == Kind.SNIPPETS;
        } catch (IllegalArgumentException e) {
            // Left out rather than failing the build, which would break every
            // application that has the definition on its class path, an
            // add-on its developer can not fix among them. Using it fails at
            // runtime with the same message.
            getLogger().error(
                    "Leaving the JavaScript of {} out of the bundle: {}",
                    definition.getName(), e.getMessage());
            return List.of();
        }
        List<Method> methods = new ArrayList<>();
        // Not getMethods(), whose inherited methods keep the annotations they
        // had before the interface declaring them was hotswapped
        for (Method method : ReflectTools.getCurrentPublicMethods(definition)) {
            if (method.isAnnotationPresent(JsExpression.class)) {
                methods.add(method);
            }
        }
        methods.sort(Comparator.comparing(JsCall::functionId));

        List<RenderedFunction> functions = new ArrayList<>();
        for (Method method : methods) {
            String rawId = JsCall.functionId(method);
            String functionId = quote(rawId);
            // The parameters of the generated function are the arguments of the
            // call, referenced as $0, $1, ... by the declared expression. The
            // last parameter of a variadic method collects the arguments that
            // follow the fixed ones into an array, as it does in Java.
            String parameters = IntStream.range(0, method.getParameterCount())
                    .mapToObj(index -> isRestParameter(method, index)
                            ? "...$" + index
                            : "$" + index)
                    .reduce((first, second) -> first + ", " + second)
                    .orElse("");
            String expression = method.getAnnotation(JsExpression.class)
                    .value();
            List<String> function = new ArrayList<>();
            if (snippets) {
                // A snippet is evaluated by what it is passed to, which may
                // need the value right away, so its function is not async.
                // Its arguments are captured, and the parameters it is called
                // with are those of a function around the expression, whose
                // `this` is whatever the invoker calls it with.
                function.add(String.format(
                        "window.Vaadin.Flow.jsSnippets[%s] = function (%s) {",
                        functionId, parameters));
                function.add(String.format("  return function (%s) {", String
                        .join(", ", JsCall.getFunctionParameterNames(method))));
                function.add(expression);
                function.add("  };");
                function.add("};");
            } else {
                // The element the definition was obtained from is the `this`
                // of a call - the same contract as an executeJs expression
                // has. The execution context, for a function that gets it,
                // leads the parameters, so `this` stays the element and $0,
                // $1, ... stay the arguments of the call
                boolean withContext = method
                        .isAnnotationPresent(JsWithExecutionContext.class);
                function.add(String.format(
                        "window.Vaadin.Flow.%s[%s] = async function (%s) {",
                        withContext ? "jsDefinitionsWithExecutionContext"
                                : "jsDefinitions",
                        functionId,
                        withContext
                                ? (parameters.isEmpty() ? EXECUTION_CONTEXT
                                        : EXECUTION_CONTEXT + ", " + parameters)
                                : parameters));
                function.add(expression);
                function.add("};");
            }
            if (withNames) {
                function.add(String.format(
                        "window.Vaadin.Flow.jsDefinitionNames[%s] = %s;",
                        functionId, quote(nameOf(definition, method))));
            }
            functions.add(new RenderedFunction(rawId, definition.getName(),
                    String.join(System.lineSeparator(), function)));
        }
        return functions;
    }

    /**
     * What a message about a call of the given method names it by: what a
     * developer wrote, rather than the hash the call itself carries.
     */
    private static String nameOf(Class<?> definition, Method method) {
        return definition.getName() + "." + method.getName() + "/"
                + method.getParameterCount();
    }

    private static boolean isRestParameter(Method method, int index) {
        return method.isVarArgs() && index == method.getParameterCount() - 1;
    }

    private static String quote(String value) {
        return "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\n", "\\n").replace("\r", "\\r") + "\"";
    }

    @Override
    protected File getGeneratedFile() {
        File frontendGeneratedDirectory = new File(
                options.getFrontendDirectory(), GENERATED);
        return new File(frontendGeneratedDirectory, JS_DEFINITIONS_FILE_NAME);
    }

    @Override
    protected boolean shouldGenerate() {
        // Whether an application declares any JavaScript is answered by
        // scanning for it, which a build that scans for anything can do
        return true;
    }
}
