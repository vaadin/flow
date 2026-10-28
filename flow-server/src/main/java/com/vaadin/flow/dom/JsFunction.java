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
package com.vaadin.flow.dom;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import org.jspecify.annotations.Nullable;

import com.vaadin.flow.internal.CspViolationReporter;
import com.vaadin.flow.internal.JacksonCodec;
import com.vaadin.flow.js.JsCall;
import com.vaadin.flow.js.JsCalledWith;
import com.vaadin.flow.js.JsDefinition;
import com.vaadin.flow.js.JsSnippet;
import com.vaadin.flow.js.JsSnippets;
import com.vaadin.flow.server.CspMode;
import com.vaadin.flow.server.CspViolationException;

/**
 * A JavaScript function value with captured parameters that can be passed to
 * {@link Element#executeJs(String, Object...)} as a parameter and reified on
 * the client as an actual callable JS function.
 * <p>
 * The {@link #getBody() body} is a JavaScript function body. Captures are
 * referenced positionally as {@code $0}, {@code $1}, &hellip; (the same naming
 * convention as {@code executeJs} parameters). Additional named runtime
 * arguments can be declared with {@link #withArguments(String...)}: the
 * materialised function then accepts them positionally and the body references
 * them by name. On the client the value materialises as a function with the
 * captures pre-bound, so the parent {@code executeJs} can invoke it as
 * {@code $N(arg1, arg2, ...)} without ever concatenating user-supplied
 * JavaScript with framework boilerplate.
 * <p>
 * Captures may be any value accepted as a parameter to
 * {@link Element#executeJs(String, Object...) executeJs}, including
 * {@link Element} (attached elements arrive as DOM nodes, detached ones as
 * {@code null}) and nested {@link JsFunction} instances. Capture types are
 * validated when the value is created.
 * <p>
 * Inside the body, {@code this} is whatever the caller of the materialised
 * function chooses (i.e. it follows normal JavaScript call semantics &ndash;
 * not the host element). To use the host element from within the body, pass it
 * as a capture.
 * <p>
 * A function created with {@link #of(String, Object...)} is compiled in the
 * browser from its body, which a content security policy that does not allow
 * <code>unsafe-eval</code> forbids. A function can instead be declared as a
 * {@link JsSnippet} by a method of a {@link JsDefinition} interface, whose
 * JavaScript the build collects into the bundle, so the browser is sent the
 * identifier of the function and the captures rather than a body:
 *
 * <pre>
 * &#64;JsDefinition
 * public interface LoggerJs extends Serializable {
 *
 *     &#64;JsExpression("console.log($0, value);")
 *     &#64;JsCalledWith("value")
 *     JsFunction createLogger(String prefix);
 * }
 *
 * JsFunction logger = JsSnippets.of(LoggerJs.class).createLogger("Greeting:");
 * element.executeJs("$0('Hello')", logger);
 * </pre>
 *
 * Such a function has no body on the server, and its runtime arguments are
 * declared by {@link JsCalledWith} rather than with
 * {@link #withArguments(String...)}.
 *
 * @author Vaadin Ltd
 * @since 25.2
 */
public final class JsFunction implements JsSnippet, Serializable {

    private final @Nullable String body;
    private final @Nullable JsCall declaredCall;
    private final List<@Nullable Object> captures;
    private final List<String> argumentNames;

    private JsFunction(@Nullable String body, @Nullable JsCall declaredCall,
            List<@Nullable Object> captures, List<String> argumentNames) {
        this.body = body;
        this.declaredCall = declaredCall;
        this.captures = captures;
        this.argumentNames = argumentNames;
    }

    /**
     * Creates a JavaScript function value with the given body and captured
     * parameters.
     *
     * @param body
     *            the JavaScript function body, with captures referenced as
     *            {@code $0}, {@code $1}, &hellip;; not {@code null}
     * @param captures
     *            the values to capture; each must be a type supported as a
     *            parameter to {@link Element#executeJs(String, Object...)}
     * @return a new {@code JsFunction} instance
     * @throws IllegalArgumentException
     *             if any capture has a type that cannot be sent to the client
     * @throws CspViolationException
     *             in the {@link CspMode#STRICT} Content Security Policy mode
     */
    public static JsFunction of(String body, @Nullable Object... captures) {
        Objects.requireNonNull(body, "body");
        Objects.requireNonNull(captures, "captures");
        CspViolationReporter.reportStringJavaScript();
        @Nullable
        Object[] copy = captures.clone();
        // Dry-run encode each capture so unsupported types fail fast. Mirrors
        // the validation done by the JavaScriptInvocation constructor for
        // executeJs parameters.
        for (@Nullable
        Object capture : copy) {
            JacksonCodec.encodeWithTypeInfo(capture);
        }
        return new JsFunction(body, null,
                Collections.unmodifiableList(Arrays.asList(copy)),
                Collections.emptyList());
    }

    /**
     * Creates the function that a call of a method of a {@link JsDefinition}
     * interface declares, which the build collects into the bundle.
     * <p>
     * For internal use only. May be renamed or removed in a future release. Get
     * the function from the implementation that {@link JsSnippets#of(Class)}
     * hands out.
     *
     * @param call
     *            the call of a method that returns a function, not
     *            <code>null</code>
     * @return a new {@code JsFunction} instance
     * @throws IllegalArgumentException
     *             if any argument of the call has a type that cannot be sent to
     *             the client
     */
    public static JsFunction ofDeclaredCall(JsCall call) {
        Objects.requireNonNull(call, "call");
        List<Object> captures = call.flattenArguments();
        // Fail fast at the call that captures an unsupported value, as of does
        for (Object capture : captures) {
            JacksonCodec.encodeWithTypeInfo(capture);
        }
        List<String> names = call.getCalledWithNames();
        return new JsFunction(null, call, captures,
                names == null ? List.of() : names);
    }

    /**
     * Gets the call of a {@link JsDefinition} method that declared this
     * function, which names the function of the bundle it runs.
     * <p>
     * For internal use only. May be renamed or removed in a future release.
     *
     * @return the declaring call, or <code>null</code> for a function created
     *         from a body with {@link #of(String, Object...)}
     */
    public @Nullable JsCall getDeclaredCall() {
        return declaredCall;
    }

    /**
     * Returns a copy of this function that declares the given names as
     * positional runtime arguments. The materialised function accepts these
     * arguments at call time, and the body references them by name.
     * <p>
     * Example:
     *
     * <pre>
     * JsFunction alerter = JsFunction.of("alert(message);")
     *         .withArguments("message");
     * element.executeJs("$0('Hello')", alerter);
     * </pre>
     *
     * @param argumentNames
     *            the names of runtime arguments, in positional order; each must
     *            be a valid JavaScript identifier
     * @return a new {@code JsFunction} with the given argument names
     * @throws IllegalStateException
     *             if argument names have already been set on this instance, or
     *             if this function is declared by a {@link JsDefinition}
     *             interface, which declares its arguments with
     *             {@link JsCalledWith}
     */
    public JsFunction withArguments(String... argumentNames) {
        if (declaredCall != null) {
            throw new IllegalStateException(
                    "The arguments of a function declared by a @JsDefinition"
                            + " interface are declared with @JsCalledWith,"
                            + " as they are part of the JavaScript in the bundle");
        }
        if (!this.argumentNames.isEmpty()) {
            throw new IllegalStateException(
                    "withArguments has already been called on this JsFunction");
        }
        return new JsFunction(body, null, captures, List.of(argumentNames));
    }

    /**
     * The JavaScript function body.
     *
     * @return the body string
     * @throws IllegalStateException
     *             if this function is declared by a {@link JsDefinition}
     *             interface, whose JavaScript is in the bundle rather than in
     *             the function
     * @deprecated The body is what the client compiles the function from, and a
     *             function declared by a {@link JsDefinition} interface, which
     *             is not compiled from a string, has none. The body of a
     *             function created with {@link #of(String, Object...)} is the
     *             one passed to it.
     */
    @Deprecated
    public String getBody() {
        if (body == null) {
            throw new IllegalStateException(
                    "A function declared by a @JsDefinition interface has no"
                            + " body, as its JavaScript is in the bundle");
        }
        return body;
    }

    /**
     * The captured values, in declaration order. For a function declared by a
     * {@link JsDefinition} interface, these are the arguments of the call that
     * declared it.
     *
     * @return an unmodifiable list of captures
     */
    public List<@Nullable Object> getCaptures() {
        return captures;
    }

    /**
     * The names of the runtime arguments declared via
     * {@link #withArguments(String...)}, or with {@link JsCalledWith} for a
     * function declared by a {@link JsDefinition} interface, in positional
     * order.
     *
     * @return an unmodifiable list of argument names; empty if none were
     *         declared
     * @deprecated The names are part of the JavaScript of the function, which
     *             is what the client compiles from them. They are the ones
     *             passed to {@link #withArguments(String...)} or declared with
     *             {@link JsCalledWith}.
     */
    @Deprecated
    public List<String> getArgumentNames() {
        return argumentNames;
    }
}
