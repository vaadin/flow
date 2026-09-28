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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.IntStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.internal.FrontendUtils;
import com.vaadin.flow.js.JsCall;
import com.vaadin.flow.js.JsDefinition;
import com.vaadin.flow.js.JsExpression;
import com.vaadin.flow.server.frontend.scanner.ClassFinder;

import static com.vaadin.flow.internal.FrontendUtils.GENERATED;
import static com.vaadin.flow.internal.FrontendUtils.JS_DEFINITIONS_FILE_NAME;

/**
 * Generates {@link FrontendUtils#JS_DEFINITIONS_FILE_NAME}, which registers the
 * JavaScript of every {@link JsDefinition} interface on the class path as an
 * ordinary function of the bundle.
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
 * For internal use only. May be renamed or removed in a future release.
 * 
 * @since 25.4
 */
public class TaskGenerateJsDefinitions extends AbstractTaskClientGenerator {

    private static final List<String> HEADER = List.of("// @ts-nocheck",
            "window.Vaadin = window.Vaadin || {};",
            "window.Vaadin.Flow = window.Vaadin.Flow || {};",
            "window.Vaadin.Flow.jsDefinitions = window.Vaadin.Flow.jsDefinitions || {};");

    // What a message about a call names it by, which is of no use to a browser
    // running the application and is left out of a production bundle
    private static final String NAMES = "window.Vaadin.Flow.jsDefinitionNames = window.Vaadin.Flow.jsDefinitionNames || {};";

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

    @Override
    protected String getFileContent() {
        // Generating the file is scanning for what goes into it, which a
        // caller that writes it again while the application runs does not do:
        // it passes the definitions in, through updateJsDefinitions
        ClassFinder classFinder = Objects.requireNonNull(
                options.getClassFinder(),
                "Generating the file needs a class finder to scan for the JavaScript definitions with");
        return renderFileContent(
                classFinder.getAnnotatedClasses(JsDefinition.class),
                !options.isProductionMode());
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
        List<String> lines = new ArrayList<>(renderHeader(withNames));
        definitions.stream().sorted(Comparator.comparing(Class::getName))
                .forEach(definition -> lines
                        .addAll(renderDefinitionLines(definition, withNames)));
        lines.addAll(FOOTER);
        return String.join(System.lineSeparator(), lines);
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
                        .stream())
                .filter(function -> !generated.contains(function))
                .flatMap(function -> Arrays
                        .stream(function.split(System.lineSeparator())))
                .toList();
        if (missing.isEmpty()) {
            return generated;
        }
        String separator = System.lineSeparator();
        String footer = String.join(separator, FOOTER);
        List<String> added = new ArrayList<>();
        if (!generated.contains(NAMES)) {
            // Written before names were registered at all, and a name is
            // assigned into an object that has to be there
            added.add(NAMES);
        }
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
                        .stream(function.split(System.lineSeparator())))
                .toList();
    }

    /**
     * What one JavaScript definition contributes, one registered function per
     * method that declares JavaScript, each as the lines it is written as.
     */
    private static List<String> renderFunctions(Class<?> definition,
            boolean withNames) {
        List<Method> methods = new ArrayList<>();
        for (Method method : definition.getMethods()) {
            if (method.isAnnotationPresent(JsExpression.class)) {
                methods.add(method);
            }
        }
        methods.sort(
                Comparator.comparing(TaskGenerateJsDefinitions::functionId));

        List<String> functions = new ArrayList<>();
        for (Method method : methods) {
            // The parameters of the generated function are the arguments of the
            // call, referenced as $0, $1, ... by the declared expression, and
            // the element the definition was obtained from is its `this` - the
            // same contract as an executeJs expression has. The last parameter
            // of a variadic method collects the arguments that follow the
            // fixed ones into an array, as it does in Java.
            String parameters = IntStream.range(0, method.getParameterCount())
                    .mapToObj(index -> isRestParameter(method, index)
                            ? "...$" + index
                            : "$" + index)
                    .reduce((first, second) -> first + ", " + second)
                    .orElse("");
            List<String> function = new ArrayList<>(List.of(String.format(
                    "window.Vaadin.Flow.jsDefinitions[%s] = async function (%s) {",
                    quote(functionId(method)), parameters),
                    method.getAnnotation(JsExpression.class).value(), "};"));
            if (withNames) {
                function.add(String.format(
                        "window.Vaadin.Flow.jsDefinitionNames[%s] = %s;",
                        quote(functionId(method)),
                        quote(nameOf(definition, method))));
            }
            functions.add(String.join(System.lineSeparator(), function));
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

    private static String functionId(Method method) {
        return JsCall.functionId(
                method.getAnnotation(JsExpression.class).value(),
                method.getParameterCount(), method.isVarArgs());
    }

    private static String quote(String value) {
        return "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
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
