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
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.IntStream;

import com.vaadin.flow.dom.JsExpression;
import com.vaadin.flow.dom.JsInvoker;
import com.vaadin.flow.dom.JsInvokerCall;
import com.vaadin.flow.internal.FrontendUtils;

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
        return fileContent(
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
    public static String fileContent(Collection<Class<?>> invokers) {
        List<String> lines = new ArrayList<>();
        lines.add("// @ts-nocheck");
        lines.add("window.Vaadin = window.Vaadin || {};");
        lines.add("window.Vaadin.Flow = window.Vaadin.Flow || {};");
        lines.add(
                "window.Vaadin.Flow.jsInvokers = window.Vaadin.Flow.jsInvokers || {};");

        invokers.stream().sorted(Comparator.comparing(Class::getName))
                .forEach(invoker -> lines.addAll(invokerLines(invoker)));

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
     * Reads back the names of the invoker interfaces a generated file
     * registers, which is what a browser that has the file can run.
     * <p>
     * Exposed together with {@link #invokerLines(Class)} so that the format
     * this class writes is also read here, and a caller which has to render the
     * file again - the hotswap path - can keep the interfaces that are in it.
     *
     * @param fileContent
     *            the content of a generated file, or <code>null</code>
     * @return the interface names the file registers, in the order it registers
     *         them
     */
    public static List<String> invokerNames(String fileContent) {
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
    public static List<String> invokerLines(Class<?> invoker) {
        List<String> lines = new ArrayList<>();
        List<Method> methods = new ArrayList<>();
        for (Method method : invoker.getMethods()) {
            if (method.isAnnotationPresent(JsExpression.class)) {
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
