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
import com.vaadin.flow.js.JsExpression;
import com.vaadin.flow.js.JsInvoker;
import com.vaadin.flow.js.JsInvokerCall;

import static com.vaadin.flow.internal.FrontendUtils.GENERATED;
import static com.vaadin.flow.internal.FrontendUtils.JS_INVOKERS_FILE_NAME;

/**
 * Generates {@link FrontendUtils#JS_INVOKERS_FILE_NAME}, which registers the
 * JavaScript of every {@link JsInvoker} interface on the class path as an
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
public class TaskGenerateJsInvokers extends AbstractTaskClientGenerator {

    private static final Pattern INVOKER_KEY = Pattern
            .compile("window\\.Vaadin\\.Flow\\.jsInvokers\\[\"([^\"]+)\"\\] =");

    private final Options options;

    TaskGenerateJsInvokers(Options options) {
        this.options = options;
    }

    @Override
    protected String getFileContent() {
        return renderFileContent(
                options.getClassFinder().getAnnotatedClasses(JsInvoker.class));
    }

    /**
     * Renders the file that registers the JavaScript of the given invoker
     * interfaces.
     * <p>
     * Exposed so that a caller which regenerates the file outside a build - the
     * hotswap path, which writes it again when an interface changed while the
     * application runs - produces exactly what a build would have written.
     *
     * @param invokers
     *            the invoker interfaces to render, not <code>null</code>
     * @return the content of the generated file
     */
    public static String renderFileContent(Collection<Class<?>> invokers) {
        List<String> lines = new ArrayList<>();
        lines.add("// @ts-nocheck");
        lines.add("window.Vaadin = window.Vaadin || {};");
        lines.add("window.Vaadin.Flow = window.Vaadin.Flow || {};");
        lines.add(
                "window.Vaadin.Flow.jsInvokers = window.Vaadin.Flow.jsInvokers || {};");

        invokers.stream().sorted(Comparator.comparing(Class::getName))
                .forEach(invoker -> lines.addAll(renderInvokerLines(invoker)));

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
     * The invoker interfaces of the given ones whose JavaScript the generated
     * file does not carry, which is the JavaScript a browser can run of them.
     *
     * @param options
     *            where the file is, not <code>null</code>
     * @param invokers
     *            the invoker interfaces to look for, not <code>null</code>
     * @return those the file does not carry, empty when it carries all of them
     */
    public static List<Class<?>> missingFromGeneratedFile(Options options,
            Collection<Class<?>> invokers) {
        String generated = readGeneratedFile(options);
        return invokers.stream()
                .filter(invoker -> !isInGeneratedFile(invoker, generated))
                .toList();
    }

    /**
     * Writes the generated file again so that it carries what the given invoker
     * interfaces declare, for a caller that has to update it while the
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
     * @param invokers
     *            the invoker interfaces to write it for, not <code>null</code>
     *            and not empty
     * @return those of them the file does not carry afterwards, empty when it
     *         carries all of them
     */
    public static List<Class<?>> updateJsInvokers(Options options,
            Collection<Class<?>> invokers) {
        String generated = readGeneratedFile(options);
        String content = renderFileContent(withInvokersOf(generated, invokers));

        TaskGenerateJsInvokers task = new TaskGenerateJsInvokers(options);
        try {
            task.writeIfChanged(task.getGeneratedFile(), content);
        } catch (IOException e) {
            getLogger().debug("Could not write {}", task.getGeneratedFile(), e);
            return List.copyOf(invokers);
        }
        return invokers.stream()
                .filter(invoker -> !isInGeneratedFile(invoker, content))
                .toList();
    }

    /**
     * Whether the given content carries what the invoker declares, compared as
     * this class renders it, so the interface name, the methods, their argument
     * counts and the JavaScript all have to match. A method that was removed
     * does not show up as a difference: its function stays in the file with
     * nothing calling it.
     */
    private static boolean isInGeneratedFile(Class<?> invoker,
            String generated) {
        if (generated == null) {
            return false;
        }
        List<String> declared = renderInvokerLines(invoker);
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
    private static Collection<Class<?>> withInvokersOf(String generated,
            Collection<Class<?>> invokers) {
        Map<String, Class<?>> byName = new LinkedHashMap<>();
        invokers.forEach(invoker -> byName.put(invoker.getName(), invoker));
        ClassLoader classLoader = invokers.iterator().next().getClassLoader();
        for (String name : readInvokerNames(generated)) {
            if (byName.containsKey(name)) {
                continue;
            }
            try {
                byName.put(name, Class.forName(name, false, classLoader));
            } catch (ClassNotFoundException | LinkageError e) {
                getLogger().debug("Could not load the invoker {}", name, e);
            }
        }
        return byName.values();
    }

    private static String readGeneratedFile(Options options) {
        File generatedFile = new TaskGenerateJsInvokers(options)
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
        return LoggerFactory.getLogger(TaskGenerateJsInvokers.class);
    }

    /**
     * Reads back the names of the invoker interfaces a generated file
     * registers, which is what a browser that has the file can run.
     * <p>
     * Exposed together with {@link #renderInvokerLines(Class)} so that the
     * format this class writes is also read here, and a caller which has to
     * render the file again - the hotswap path - can keep the interfaces that
     * are in it.
     *
     * @param fileContent
     *            the content of a generated file, or <code>null</code>
     * @return the interface names the file registers, in the order it registers
     *         them
     */
    public static List<String> readInvokerNames(String fileContent) {
        List<String> names = new ArrayList<>();
        if (fileContent == null) {
            return names;
        }
        Matcher matcher = INVOKER_KEY.matcher(fileContent);
        while (matcher.find()) {
            String name = matcher.group(1);
            if (!names.contains(name)) {
                names.add(name);
            }
        }
        return names;
    }

    /**
     * Renders what one invoker interface contributes to the generated file: the
     * registration of its interface name, and one function per method that
     * declares JavaScript, keyed by method name and argument count.
     * <p>
     * Exposed so that a caller which has to tell whether a bundle carries what
     * an interface declares - the hotswap path, which compares the two - reads
     * the same rendering the build wrote, instead of matching parts of it.
     *
     * @param invoker
     *            the invoker interface to render, not <code>null</code>
     * @return the lines this invoker contributes, empty if it declares no
     *         JavaScript
     */
    public static List<String> renderInvokerLines(Class<?> invoker) {
        List<String> lines = new ArrayList<>();
        List<Method> methods = new ArrayList<>();
        for (Method method : invoker.getMethods()) {
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
        methods.sort(Comparator.comparing(TaskGenerateJsInvokers::methodId));

        lines.add(String.format(
                "window.Vaadin.Flow.jsInvokers[%s] = Object.assign(window.Vaadin.Flow.jsInvokers[%s] || {}, {",
                quote(invoker.getName()), quote(invoker.getName())));
        for (Method method : methods) {
            // The parameters of the generated function are the arguments of the
            // call, referenced as $0, $1, ... by the declared expression, and
            // the element the invoker was obtained from is its `this` - the
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
        return JsInvokerCall.methodId(method.getName(),
                method.getParameterCount());
    }

    private static String quote(String value) {
        return "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }

    @Override
    protected File getGeneratedFile() {
        File frontendGeneratedDirectory = new File(
                options.getFrontendDirectory(), GENERATED);
        return new File(frontendGeneratedDirectory, JS_INVOKERS_FILE_NAME);
    }

    @Override
    protected boolean shouldGenerate() {
        return options.getClassFinder() != null;
    }
}
