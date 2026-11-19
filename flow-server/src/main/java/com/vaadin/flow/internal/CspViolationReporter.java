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

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;

import org.jspecify.annotations.Nullable;
import org.slf4j.LoggerFactory;

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

    // Bounded by the number of call sites in the code, and of the details
    // reported from them, for each service
    private static final Map<VaadinService, Set<String>> warnedCalls = Collections
            .synchronizedMap(new WeakHashMap<>());

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
     * The message adds the method that, declared in a
     * {@link com.vaadin.flow.js.JsStringFallback} interface, would run the
     * expression from the bundle instead, ready to paste. A warning is logged
     * once per call site and expression, so a call site that sends different
     * strings, such as one that concatenates values into them, is reported for
     * each of them.
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
        report(null, new StringCall(expression, parameterCount));
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
     * A call that sends an expression with a number of parameters, which a
     * declared method can stand in for.
     */
    private record StringCall(String expression, int parameterCount) {
    }

    private static void report(@Nullable String detail,
            @Nullable StringCall call) {
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
        if (call != null) {
            message += describeFallback(call, stack);
        }
        CspViolationException violation = new CspViolationException(message);
        violation.setStackTrace(stack);

        if (mode == CspMode.STRICT) {
            throw violation;
        }
        // Different details or expressions from one call site are different
        // reports
        String key = getCallSite(stack);
        if (detail != null) {
            key = detail + '\n' + key;
        }
        if (call != null) {
            key = call.parameterCount() + ":" + call.expression() + '\n' + key;
        }
        if (warnedCalls
                .computeIfAbsent(service, k -> ConcurrentHashMap.newKeySet())
                .add(key)) {
            LoggerFactory.getLogger(CspViolationReporter.class).warn(message,
                    violation);
        }
    }

    /**
     * The method that would stand in for a string call, named after the
     * application code that made it, so that methods pasted for different call
     * sites do not get the same name.
     */
    private static String describeFallback(StringCall call,
            StackTraceElement[] stack) {
        StringBuilder parameters = new StringBuilder();
        for (int index = 0; index < call.parameterCount(); index++) {
            parameters.append(index == 0 ? "" : ", ").append("Object arg")
                    .append(index);
        }
        return """
                 To run it from the bundle instead, declare this method in a \
                @JsDefinition interface that is marked with @JsStringFallback:

                    @JsExpression("%s")
                    PendingJavaScriptResult %s(%s);
                """.formatted(escapeJavaString(call.expression()),
                suggestMethodName(stack), parameters);
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
            String name = Character.toLowerCase(simpleName.charAt(0))
                    + simpleName.substring(1)
                    + (method.isEmpty() ? ""
                            : Character.toUpperCase(method.charAt(0))
                                    + method.substring(1));
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
