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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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
 * anything from a string: the server sends which interface and method to run,
 * and the function is already in the bundle, so the call survives a content
 * security policy that does not allow <code>unsafe-eval</code> and the
 * JavaScript an application can be made to run is known when it is built.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 */
public class TaskGenerateJsDefinitions extends AbstractTaskClientGenerator {

    private static final Pattern DEFINITION_KEY = Pattern.compile(
            "window\\.Vaadin\\.Flow\\.jsDefinitions\\[\"([^\"]+)\"\\] =");

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
        List<String> lines = new ArrayList<>();
        lines.add("// @ts-nocheck");
        lines.add("window.Vaadin = window.Vaadin || {};");
        lines.add("window.Vaadin.Flow = window.Vaadin.Flow || {};");
        lines.add(
                "window.Vaadin.Flow.jsDefinitions = window.Vaadin.Flow.jsDefinitions || {};");

        definitions.stream().sorted(Comparator.comparing(Class::getName))
                .forEach(definition -> lines
                        .addAll(renderDefinitionLines(definition)));

        // Writing this file again while the application runs replaces it in the
        // browser that has it: everything above only writes into the registry,
        // so the module can accept its own update and nothing else has to be
        // reloaded for a changed declaration to take effect. The dev server
        // drops the block from a production build, where import.meta.hot is
        // not defined.
        lines.add("if (import.meta.hot) {");
        lines.add("  import.meta.hot.accept();");
        lines.add("}");

        // See https://github.com/vaadin/flow/issues/14184
        lines.add("export {};");

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
    public static List<Class<?>> missingFromGeneratedFile(Options options,
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
     * The interfaces the file already registers are kept: they are what a
     * browser that has the file can run, and the caller only knows about the
     * ones it passes in. One the file registers and the application no longer
     * has is dropped, and one whose annotation was removed keeps its functions
     * with nothing calling them, until a build renders the file again.
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
        String content = renderFileContent(
                withDefinitionsOf(generated, definitions));

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
     * as this class renders it, so the interface name, the methods, their
     * argument counts and the JavaScript all have to match. A method that was
     * removed does not show up as a difference: its function stays in the file
     * with nothing calling it.
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
     * The given interfaces, plus the ones the content registers that are not
     * among them and can still be loaded.
     */
    private static Collection<Class<?>> withDefinitionsOf(String generated,
            Collection<Class<?>> definitions) {
        Map<String, Class<?>> byName = new LinkedHashMap<>();
        definitions.forEach(
                definition -> byName.put(definition.getName(), definition));
        ClassLoader classLoader = definitions.iterator().next()
                .getClassLoader();
        for (String name : readDefinitionNames(generated)) {
            if (byName.containsKey(name)) {
                continue;
            }
            try {
                byName.put(name, Class.forName(name, false, classLoader));
            } catch (ClassNotFoundException | LinkageError e) {
                getLogger().debug("Could not load the definition {}", name, e);
            }
        }
        return byName.values();
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
     * Reads back the names of the JavaScript definitions a generated file
     * registers, which is what a browser that has the file can run. Reading the
     * format back here keeps it next to {@link #renderDefinitionLines(Class)},
     * which writes it.
     *
     * @param fileContent
     *            the content of a generated file, or <code>null</code>
     * @return the interface names the file registers, in the order it registers
     *         them
     */
    private static List<String> readDefinitionNames(String fileContent) {
        List<String> names = new ArrayList<>();
        if (fileContent == null) {
            return names;
        }
        Matcher matcher = DEFINITION_KEY.matcher(fileContent);
        while (matcher.find()) {
            String name = matcher.group(1);
            if (!names.contains(name)) {
                names.add(name);
            }
        }
        return names;
    }

    /**
     * Renders what one JavaScript definition contributes to the generated file:
     * the registration of its interface name, and one function per method that
     * declares JavaScript, keyed by method name and argument count.
     * <p>
     * Package private: whether a file carries what an interface declares is
     * answered by {@link #missingFromGeneratedFile(Options, Collection)}, which
     * compares against this.
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
            // A default method runs in Java, so it has nothing in the bundle
            // even if it carries the annotation
            if (method.isAnnotationPresent(JsExpression.class)
                    && !method.isDefault()) {
                methods.add(method);
            }
        }
        if (methods.isEmpty()) {
            return lines;
        }
        methods.sort(Comparator.comparing(TaskGenerateJsDefinitions::methodId));

        lines.add(String.format(
                "window.Vaadin.Flow.jsDefinitions[%s] = Object.assign(window.Vaadin.Flow.jsDefinitions[%s] || {}, {",
                quote(definition.getName()), quote(definition.getName())));
        for (Method method : methods) {
            // The parameters of the generated function are the arguments of the
            // call, referenced as $0, $1, ... by the declared expression, and
            // the element the definition was obtained from is its `this` - the
            // same contract as an executeJs expression has.
            String parameters = IntStream.range(0, method.getParameterCount())
                    .mapToObj(index -> "$" + index)
                    .reduce((first, second) -> first + ", " + second)
                    .orElse("");
            lines.add(String.format("  %s: async function (%s) {",
                    quote(methodId(method)), parameters));
            lines.add(method.getAnnotation(JsExpression.class).value());
            lines.add("  },");
        }
        lines.add("});");
        return lines;
    }

    private static String methodId(Method method) {
        return JsCall.methodId(method.getName(), method.getParameterCount());
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
