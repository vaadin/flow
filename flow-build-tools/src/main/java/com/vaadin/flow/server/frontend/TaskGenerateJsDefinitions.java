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
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.stream.IntStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.internal.FrontendUtils;
import com.vaadin.flow.js.JsCall;
import com.vaadin.flow.js.JsDefinition;
import com.vaadin.flow.js.JsExpression;

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
 * For internal use only. May be renamed or removed in a future release.
 */
public class TaskGenerateJsDefinitions extends AbstractTaskClientGenerator {

    private static final List<String> HEADER = List.of("// @ts-nocheck",
            "window.Vaadin = window.Vaadin || {};",
            "window.Vaadin.Flow = window.Vaadin.Flow || {};",
            "window.Vaadin.Flow.jsDefinitions = window.Vaadin.Flow.jsDefinitions || {};");

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
        return renderFileContent(options.getClassFinder()
                .getAnnotatedClasses(JsDefinition.class));
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
     * @return the content of the generated file
     */
    static String renderFileContent(Collection<Class<?>> definitions) {
        List<String> lines = new ArrayList<>(HEADER);
        definitions.stream().sorted(Comparator.comparing(Class::getName))
                .forEach(definition -> lines
                        .addAll(renderDefinitionLines(definition)));
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
     * as this class renders it, so the JavaScript of every method and the
     * number of arguments it takes have to match. A method that was removed
     * does not show up as a difference: its function stays in the file with
     * nothing calling it.
     */
    private static boolean isInGeneratedFile(Class<?> definition,
            String generated) {
        if (generated == null) {
            return false;
        }
        List<String> declared = renderDefinitionLines(definition);
        if (declared.isEmpty()) {
            // Declares no JavaScript, so there is nothing to carry
            return true;
        }
        return generated
                .contains(String.join(System.lineSeparator(), declared));
    }

    /**
     * The given content with the entries of the given definitions that it does
     * not hold yet put in front of what closes the file, or the whole file
     * rendered when there is no content to add to.
     */
    private static String withMissingEntries(String generated,
            Collection<Class<?>> definitions) {
        List<String> missing = definitions.stream()
                .sorted(Comparator.comparing(Class::getName))
                .filter(definition -> !isInGeneratedFile(definition, generated))
                .flatMap(definition -> renderDefinitionLines(definition)
                        .stream())
                .toList();
        String footer = String.join(System.lineSeparator(), FOOTER);
        if (generated == null || !generated.contains(footer)) {
            // Nothing to add to, or something else than this class wrote it
            return renderFileContent(definitions);
        }
        if (missing.isEmpty()) {
            return generated;
        }
        return generated.replace(footer,
                String.join(System.lineSeparator(), missing)
                        + System.lineSeparator() + footer);
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
    static List<String> renderDefinitionLines(Class<?> definition) {
        List<String> lines = new ArrayList<>();
        List<Method> methods = new ArrayList<>();
        for (Method method : definition.getMethods()) {
            if (method.isAnnotationPresent(JsExpression.class)) {
                methods.add(method);
            }
        }
        if (methods.isEmpty()) {
            return lines;
        }
        methods.sort(
                Comparator.comparing(TaskGenerateJsDefinitions::functionId));

        for (Method method : methods) {
            // The parameters of the generated function are the arguments of the
            // call, referenced as $0, $1, ... by the declared expression, and
            // the element the definition was obtained from is its `this` - the
            // same contract as an executeJs expression has.
            String parameters = IntStream.range(0, method.getParameterCount())
                    .mapToObj(index -> "$" + index)
                    .reduce((first, second) -> first + ", " + second)
                    .orElse("");
            lines.add(String.format(
                    "window.Vaadin.Flow.jsDefinitions[%s] = async function (%s) {",
                    quote(functionId(method)), parameters));
            lines.add(method.getAnnotation(JsExpression.class).value());
            lines.add("};");
        }
        return lines;
    }

    private static String functionId(Method method) {
        return JsCall.functionId(
                method.getAnnotation(JsExpression.class).value(),
                method.getParameterCount());
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
        return options.getClassFinder() != null;
    }
}
