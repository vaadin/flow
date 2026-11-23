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
package com.vaadin.flow.internal;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.jspecify.annotations.Nullable;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.internal.BundledEventData;
import com.vaadin.flow.component.page.Page;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.function.DeploymentConfiguration;
import com.vaadin.flow.server.CspMode;
import com.vaadin.flow.server.CspViolationException;
import com.vaadin.flow.server.VaadinService;

/**
 * Reports calls to APIs that send JavaScript to the browser as a string, which
 * the browser compiles with <code>new Function</code> and a strict Content
 * Security Policy without <code>unsafe-eval</code> blocks.
 * <p>
 * What happens depends on the {@link CspMode} of the current service: nothing
 * in {@link CspMode#OFF}, a warning logged once per call site and detail in
 * {@link CspMode#WARN}, and a {@link CspViolationException} in
 * {@link CspMode#STRICT}. Every string-based call is reported, including the
 * ones that Vaadin itself makes. Both the warning and the exception carry a
 * stack trace that starts at the API and leads to the code that called it.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 * 
 * @since 25.4
 */
public final class CspViolationReporter {

    private static final String MESSAGE = "JavaScript is sent to the browser as a string, which the browser has to compile and a Content Security Policy without 'unsafe-eval' blocks. The stack trace leads from the API to the code that called it.";

    // What each call site has logged, by the detail and the JavaScript it was
    // for. Bounded by the number of call sites in the code, times
    // MAX_LOGGED_PER_CALL_SITE, for each service
    private static final Map<VaadinService, Map<String, Set<String>>> warnedCalls = Collections
            .synchronizedMap(new WeakHashMap<>());

    // A call site that builds its string from changing values would otherwise
    // log, and keep, one entry for each value
    private static final int MAX_LOGGED_PER_CALL_SITE = 10;

    private static final String NO_MORE_FROM_CALL_SITE = """
            This call site has now logged %d different reports, so further \
            ones from it are not logged.""".formatted(MAX_LOGGED_PER_CALL_SITE);

    // The first JavaScript that each call site reported, to tell when a call
    // site sends different strings. Bounded by the number of call sites, for
    // each service
    private static final Map<VaadinService, Map<String, String>> firstJavaScript = Collections
            .synchronizedMap(new WeakHashMap<>());

    private static final String VARYING_JAVASCRIPT = """
            This call site has sent different JavaScript before, so the string \
            is probably built from values that change. A declaration matches \
            one exact string only, so @JsStringFallback does not support \
            strings like this. In your own code, declare the JavaScript once \
            and pass the changing values as parameters ($0, $1, ...). For an \
            add-on, the add-on itself has to change: ask its maintainers to \
            pass the values as parameters or to use a @JsDefinition.
            """;

    private CspViolationReporter() {
        // Only static helpers
    }

    /**
     * Reports a call to an API that sends JavaScript to the browser as a
     * string. Called by the API itself, which is where the stack trace of the
     * report starts.
     * <p>
     * A warning is logged once per call site. Use
     * {@link #reportStringJavaScript(String)} when the call site does not tell
     * what is reported, e.g. when a component reports from an action that runs
     * before the client response.
     *
     * @throws CspViolationException
     *             in the {@link CspMode#STRICT} mode
     */
    public static void reportStringJavaScript() {
        report(null, null);
    }

    /**
     * Reports a call to an API that sends JavaScript to the browser as a
     * string, with a detail that tells what is reported. Called by the API
     * itself, which is where the stack trace of the report starts.
     * <p>
     * The detail is added to the message, and a warning is logged once per call
     * site and detail. This identifies the report when the call site alone does
     * not, e.g. when a component reports from an action that runs before the
     * client response, where the stack does not reach the application code that
     * set the JavaScript. Use {@link #reportStringJavaScript()} when the call
     * site is enough.
     *
     * @param detail
     *            a short description of what is reported, e.g. the path of the
     *            option that has the JavaScript, not <code>null</code>
     * @throws CspViolationException
     *             in the {@link CspMode#STRICT} mode
     */
    public static void reportStringJavaScript(String detail) {
        Objects.requireNonNull(detail, "The detail must not be null");
        report(detail, null);
    }

    /**
     * Reports a call to <code>executeJs</code> with a string expression. Called
     * by the API itself, which is where the stack trace of the report starts.
     * <p>
     * The message adds a declared method for the expression, ready to paste,
     * and says how to use it: called through <code>executeJs(Class)</code> when
     * the call is in code the developer can change, or in an interface marked
     * with {@link com.vaadin.flow.js.JsStringFallback} when it is in an add-on
     * that the developer can not change. A warning is logged once per call site
     * and expression, so a call site that sends different strings, such as one
     * that concatenates values into them, is reported for each of them.
     *
     * @param expression
     *            the expression that is sent, not <code>null</code>
     * @param parameterCount
     *            the number of parameters the expression is sent with
     * @throws CspViolationException
     *             in the {@link CspMode#STRICT} mode
     */
    public static void reportStringExecuteJs(String expression,
            int parameterCount) {
        Objects.requireNonNull(expression, "The expression must not be null");
        report(null, new ExecuteJsCall(expression, parameterCount));
    }

    /**
     * Reports a function created from a string body, as
     * <code>JsFunction.of</code> and the string <code>addJsInitializer</code>
     * do. Called by the API itself, which is where the stack trace of the
     * report starts.
     * <p>
     * Like {@link #reportStringExecuteJs(String, int)}, the message adds the
     * declared method for the body and says how to use it.
     *
     * @param body
     *            the body of the function, not <code>null</code>
     * @param captureCount
     *            the number of values the function captures
     * @throws CspViolationException
     *             in the {@link CspMode#STRICT} mode
     */
    public static void reportStringFunction(String body, int captureCount) {
        Objects.requireNonNull(body, "The body must not be null");
        report(null, new FunctionBody(body, captureCount, List.of(), false));
    }

    /**
     * Reports a function created from a string body that is then given the
     * names of its runtime arguments with
     * <code>JsFunction.withArguments</code>. Called by the API itself, which is
     * where the stack trace of the report starts.
     * <p>
     * Like {@link #reportStringFunction(String, int)}, the message adds the
     * declared method for the body, here with the names in
     * <code>@JsFunctionParameterNames</code>.
     *
     * @param body
     *            the body of the function, not <code>null</code>
     * @param captureCount
     *            the number of values the function captures
     * @param argumentNames
     *            the names of the runtime arguments, not <code>null</code>
     * @throws CspViolationException
     *             in the {@link CspMode#STRICT} mode
     */
    public static void reportStringFunction(String body, int captureCount,
            List<String> argumentNames) {
        Objects.requireNonNull(body, "The body must not be null");
        Objects.requireNonNull(argumentNames,
                "The argument names must not be null");
        report(null, new FunctionBody(body, captureCount,
                List.copyOf(argumentNames), false));
    }

    /**
     * Reports a dynamic import given as a string expression. Called by the API
     * itself, which is where the stack trace of the report starts.
     * <p>
     * Like {@link #reportStringExecuteJs(String, int)}, the message adds the
     * declared method for the expression and says how to use it.
     *
     * @param expression
     *            the expression that returns a <code>Promise</code>, not
     *            <code>null</code>
     * @throws CspViolationException
     *             in the {@link CspMode#STRICT} mode
     */
    public static void reportStringDynamicImport(String expression) {
        Objects.requireNonNull(expression, "The expression must not be null");
        report(null, new FunctionBody(expression, 0, List.of(), true));
    }

    /**
     * Reports an event filter given as a string expression. Called by the API
     * itself, which is where the stack trace of the report starts.
     * <p>
     * The message says how to declare the filter: as a filter snippet when the
     * call is in code the developer can change, or in
     * {@link com.vaadin.flow.js.JsStringFallback#filters()} when it is in an
     * add-on.
     *
     * @param filter
     *            the filter, not <code>null</code>
     * @throws CspViolationException
     *             in the {@link CspMode#STRICT} mode
     */
    public static void reportStringFilter(String filter) {
        Objects.requireNonNull(filter, "The filter must not be null");
        report(null, new Filter(filter));
    }

    /**
     * Reports event data values given as string expressions. Called by the API
     * itself, which is where the stack trace of the report starts.
     * <p>
     * The message says how to declare the values: as event data snippets when
     * the call is in code the developer can change, or in
     * {@link com.vaadin.flow.js.JsStringFallback#eventData()} when it is in an
     * add-on.
     *
     * @param eventData
     *            the values the browser would compile, possibly mapped to an
     *            element, not <code>null</code> and not empty
     * @throws CspViolationException
     *             in the {@link CspMode#STRICT} mode
     */
    public static void reportStringEventData(List<String> eventData) {
        Objects.requireNonNull(eventData, "The event data must not be null");
        report(null, new EventData(eventData.stream()
                .map(BundledEventData::getExpression).toList()));
    }

    /**
     * Checks whether string JavaScript is reported for the current service,
     * i.e. whether its {@link CspMode} is {@link CspMode#WARN} or
     * {@link CspMode#STRICT}. A caller that does extra work to collect what it
     * reports, e.g. to find the detail for
     * {@link #reportStringJavaScript(String)}, can skip that work when this
     * returns <code>false</code>.
     *
     * @return <code>true</code> if reports are logged or thrown,
     *         <code>false</code> if they are ignored
     */
    public static boolean isEnabled() {
        return isEnabled(getCspMode(VaadinService.getCurrent()));
    }

    private static boolean isEnabled(CspMode mode) {
        return (mode == CspMode.WARN) || (mode == CspMode.STRICT);
    }

    private static CspMode getCspMode(VaadinService service) {
        // No service outside of a request or UI.access, e.g. for a component
        // built in a background thread before it is attached, and no
        // configuration for a service created with the no-argument
        // constructor: there is no mode to apply then
        DeploymentConfiguration configuration = (service == null) ? null
                : service.getDeploymentConfiguration();
        return (configuration == null) ? CspMode.OFF
                : configuration.getCspMode();
    }

    /**
     * JavaScript sent as a string that the message can say how to declare.
     * <p>
     * Whether the call is in code the developer can change decides between
     * using the declared JavaScript directly and declaring a fallback for the
     * string. The stack can not tell: an add-on and the application are both
     * application code there, and an application can have its own code in jars.
     * So each message describes both.
     */
    private sealed interface StringJavaScript {

        /**
         * What tells this JavaScript apart from other JavaScript reported from
         * the same call site.
         */
        String key();

        /**
         * The part of the message that says how to declare the JavaScript.
         *
         * @param name
         *            the name to suggest for the declared method
         * @param stack
         *            the stack, starting at the API
         */
        String describeFix(String name, StackTraceElement[] stack);
    }

    private record ExecuteJsCall(String expression,
            int parameterCount) implements StringJavaScript {

        @Override
        public String key() {
            return "executeJs " + parameterCount + ":" + expression;
        }

        @Override
        public String describeFix(String name, StackTraceElement[] stack) {
            // The API is the first frame: Element.executeJs or Page.executeJs
            String target = stack[0].getClassName().equals(Page.class.getName())
                    ? "page"
                    : "element";
            return """
                     Declare the JavaScript in a @JsDefinition interface, for \
                    example:

                        @JsDefinition
                        public interface YourJs extends JsInvokers {
                            @JsExpression("%s")
                            PendingJavaScriptResult %s(%s);
                        }

                    If this call is in your own code, call that method instead \
                    of sending the string: %s.executeJs(YourJs.class).%s(%s). \
                    If the call is in an add-on that you can not change, also \
                    add @JsStringFallback to the interface, next to \
                    @JsDefinition. Then the string call runs the method from \
                    the bundle.
                    """.formatted(escapeJavaString(expression), name,
                    declareParameters(parameterCount), target, name,
                    passArguments(parameterCount));
        }
    }

    private record FunctionBody(String body, int captureCount,
            List<String> argumentNames,
            boolean dynamicImport) implements StringJavaScript {

        @Override
        public String key() {
            return "function " + captureCount + argumentNames + ":" + body;
        }

        @Override
        public String describeFix(String name, StackTraceElement[] stack) {
            String function = "JsSnippets.of(YourFunctions.class)." + name + "("
                    + passArguments(captureCount) + ")";
            String use;
            if (dynamicImport) {
                use = "page.addDynamicImport(" + function + ")";
            } else if (isCalledBy(stack, Element.class, "addJsInitializer")) {
                use = "element.addJsInitializer(" + function + ")";
            } else {
                use = function;
            }
            return """
                     Declare the function in a @JsDefinition interface, for \
                    example:

                        @JsDefinition
                        public interface YourFunctions extends JsSnippets {
                            @JsExpression("%s")
                    %s        JsFunction %s(%s);
                        }

                    If this call is in your own code, create the function from \
                    that method instead of from the string: %s. If the call is \
                    in an add-on that you can not change, also add \
                    @JsStringFallback to the interface, next to @JsDefinition. \
                    Then the string runs the function from the bundle.%s
                    """.formatted(escapeJavaString(body), declareNames(), name,
                    declareParameters(captureCount), use, namesHint());
        }

        private String declareNames() {
            if (argumentNames.isEmpty()) {
                return "";
            }
            return "        @JsFunctionParameterNames({ " + argumentNames
                    .stream().map(argument -> "\"" + argument + "\"")
                    .collect(Collectors.joining(", ")) + " })\n";
        }

        private String namesHint() {
            if (dynamicImport || !argumentNames.isEmpty()) {
                return "";
            }
            return " If the code then calls withArguments on the function, "
                    + "add @JsFunctionParameterNames with the same names to "
                    + "the method.";
        }

        /**
         * Whether the given method of the given class is between the API and
         * the application code, such as the string initializer of an element
         * that creates its function with <code>JsFunction.of</code>.
         */
        private static boolean isCalledBy(StackTraceElement[] stack,
                Class<?> type, String method) {
            for (StackTraceElement frame : stack) {
                if (frame.getClassName().equals(type.getName())
                        && frame.getMethodName().equals(method)) {
                    return true;
                }
                if (StackTraceUtil.isApplicationClass(frame.getClassName())) {
                    return false;
                }
            }
            return false;
        }
    }

    private record Filter(String filter) implements StringJavaScript {

        @Override
        public String key() {
            return "filter " + filter;
        }

        @Override
        public String describeFix(String name, StackTraceElement[] stack) {
            String escaped = escapeJavaString(filter);
            return """
                     If this call is in your own code, declare the filter in a \
                    @JsDefinition interface and set it with \
                    registration.setFilterExpression(JsSnippets.of(YourEventJs.class).%s()) \
                    instead of setFilter:

                        @JsDefinition
                        public interface YourEventJs extends JsSnippets {
                            @JsExpression("return (%s);")
                            DomEventFilter %s();
                        }

                    If the call is in an add-on that you can not change, list \
                    the filter on a @JsDefinition interface instead. Then \
                    setting it runs a function from the bundle:

                        @JsDefinition
                        @JsStringFallback(filters = "%s")
                        public interface YourFallbacks extends JsSnippets {
                        }
                    """
                    .formatted(name, escaped, name, escaped);
        }
    }

    private record EventData(List<String> values) implements StringJavaScript {

        @Override
        public String key() {
            return "eventData " + String.join("\n", values);
        }

        @Override
        public String describeFix(String name, StackTraceElement[] stack) {
            StringBuilder methods = new StringBuilder();
            List<String> names = new ArrayList<>();
            for (int index = 0; index < values.size(); index++) {
                String methodName = index == 0 ? name : (name + (index + 1));
                names.add(methodName);
                methods.append(index == 0 ? "" : "\n").append("""
                                @JsExpression("return (%s);")
                                DomEventData<Object> %s();
                        """.formatted(escapeJavaString(values.get(index)),
                        methodName));
            }
            String listed = values.stream()
                    .map(value -> "\"" + escapeJavaString(value) + "\"")
                    .collect(Collectors.joining(", "));
            if (values.size() > 1) {
                listed = "{ " + listed + " }";
            }
            return """
                     If this call is in your own code, declare the event data \
                    in a @JsDefinition interface. Keep what \
                    JsSnippets.of(YourEventJs.class).%s() returns, add it with \
                    registration.addEventData instead of the string, and read \
                    the value with event.getEventData for the same object:

                        @JsDefinition
                        public interface YourEventJs extends JsSnippets {
                    %s    }

                    If the call is in an add-on that you can not change, list \
                    the event data on a @JsDefinition interface instead. Then \
                    adding it runs a function from the bundle, and the value is \
                    read under the same string as before:

                        @JsDefinition
                        @JsStringFallback(eventData = %s)
                        public interface YourFallbacks extends JsSnippets {
                        }
                    """
                    .formatted(String.join("(), ", names), methods, listed);
        }
    }

    private static void report(@Nullable String detail,
            @Nullable StringJavaScript javaScript) {
        VaadinService service = VaadinService.getCurrent();
        CspMode mode = getCspMode(service);
        if (!isEnabled(mode)) {
            return;
        }

        StackTraceElement[] fullStack = new Throwable().getStackTrace();
        // Start the trace at the API rather than at the two methods of this
        // class
        StackTraceElement[] stack = Arrays.copyOfRange(fullStack, 2,
                fullStack.length);
        String message = MESSAGE;
        if (detail != null) {
            message += " The JavaScript is in \"" + detail + "\".";
        }
        if (javaScript != null) {
            message += javaScript.describeFix(suggestMethodName(stack), stack);
            String first = firstJavaScript
                    .computeIfAbsent(service, k -> new ConcurrentHashMap<>())
                    .putIfAbsent(getCallSite(stack), javaScript.key());
            if (first != null && !first.equals(javaScript.key())) {
                message += "\n" + VARYING_JAVASCRIPT;
            }
        }
        CspViolationException violation = new CspViolationException(message);
        violation.setStackTrace(stack);

        if (mode == CspMode.STRICT) {
            throw violation;
        }
        // Different details or JavaScript from one call site are different
        // reports, since each needs its own declaration
        String report = (detail == null ? "" : detail) + '\n'
                + (javaScript == null ? "" : javaScript.key());
        Set<String> logged = warnedCalls
                .computeIfAbsent(service, k -> new ConcurrentHashMap<>())
                .computeIfAbsent(getCallSite(stack), k -> new HashSet<>());
        synchronized (logged) {
            if (logged.size() >= MAX_LOGGED_PER_CALL_SITE
                    || !logged.add(report)) {
                return;
            }
            if (logged.size() == MAX_LOGGED_PER_CALL_SITE) {
                message += "\n" + NO_MORE_FROM_CALL_SITE;
            }
        }
        LoggerFactory.getLogger(CspViolationReporter.class).warn(message,
                violation);
    }

    private static String declareParameters(int count) {
        return IntStream.range(0, count).mapToObj(index -> "Object arg" + index)
                .collect(Collectors.joining(", "));
    }

    private static String passArguments(int count) {
        return IntStream.range(0, count).mapToObj(index -> "arg" + index)
                .collect(Collectors.joining(", "));
    }

    private static String suggestMethodName(StackTraceElement[] stack) {
        for (StackTraceElement frame : stack) {
            if (!StackTraceUtil.isApplicationClass(frame.getClassName())) {
                continue;
            }
            String className = frame.getClassName();
            String simpleName = className
                    .substring(className.lastIndexOf('.') + 1);
            // A nested class names itself, an anonymous one its outer class
            for (String part : simpleName.split("\\$")) {
                if (!part.isEmpty() && !Character.isDigit(part.charAt(0))) {
                    simpleName = part;
                }
            }
            String method = frame.getMethodName();
            if (method.startsWith("lambda$")) {
                // lambda$enclosingMethod$0
                method = method.split("\\$")[1];
            }
            if (method.startsWith("<")) {
                // A constructor or static initializer adds nothing to the name
                method = "";
            }
            String capitalizedMethod = method.isEmpty() ? ""
                    : (Character.toUpperCase(method.charAt(0))
                            + method.substring(1));
            String name = Character.toLowerCase(simpleName.charAt(0))
                    + simpleName.substring(1) + capitalizedMethod;
            return isJavaIdentifier(name) ? name : "call";
        }
        return "call";
    }

    private static boolean isJavaIdentifier(String name) {
        if (name.isEmpty()
                || !Character.isJavaIdentifierStart(name.charAt(0))) {
            return false;
        }
        return name.chars().allMatch(Character::isJavaIdentifierPart);
    }

    private static String escapeJavaString(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t");
    }

    /**
     * Identifies a call site by the frames from the API up to and including the
     * first frame of application code, so that the same application line
     * reaching the API through different framework code counts as different
     * call sites.
     */
    private static String getCallSite(StackTraceElement[] stack) {
        StringBuilder callSite = new StringBuilder();
        for (StackTraceElement frame : stack) {
            callSite.append(frame).append('\n');
            if (StackTraceUtil.isApplicationClass(frame.getClassName())) {
                break;
            }
        }
        return callSite.toString();
    }
}
