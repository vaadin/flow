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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import org.jspecify.annotations.Nullable;

import com.vaadin.flow.internal.JacksonCodec;

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
 *
 * @author Vaadin Ltd
 */
public final class JsFunction implements Serializable {

    private final String body;
    private final List<@Nullable Object> captures;
    private final List<String> argumentNames;
    private final List<String> parameterNames;

    private JsFunction(String body, List<@Nullable Object> captures,
            List<String> argumentNames, List<String> parameterNames) {
        this.body = body;
        this.captures = captures;
        this.argumentNames = argumentNames;
        this.parameterNames = parameterNames;
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
     */
    public static JsFunction of(String body, @Nullable Object... captures) {
        Objects.requireNonNull(body, "body");
        Objects.requireNonNull(captures, "captures");
        @Nullable
        Object[] copy = captures.clone();
        // Dry-run encode each capture so unsupported types fail fast. Mirrors
        // the validation done by the JavaScriptInvocation constructor for
        // executeJs parameters.
        for (@Nullable
        Object capture : copy) {
            JacksonCodec.encodeWithTypeInfo(capture);
        }
        return new JsFunction(body,
                Collections.unmodifiableList(Arrays.asList(copy)),
                Collections.emptyList(), Collections.emptyList());
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
     *             if argument names have already been set on this instance
     */
    public JsFunction withArguments(String... argumentNames) {
        if (!this.argumentNames.isEmpty()) {
            throw new IllegalStateException(
                    "withArguments has already been called on this JsFunction");
        }
        return new JsFunction(body, captures, List.of(argumentNames),
                parameterNames);
    }

    /**
     * Returns a copy of this function with an additional named capture. The
     * function body can reference the value as {@code parameterName}; the
     * implementation prepends {@code let <parameterName>=$<index>;} to the body
     * and appends the value to the captures list (so positional {@code $N}
     * access continues to work for captures added via
     * {@link #of(String, Object...)}).
     * <p>
     * Each call adds one parameter; calls may be chained. Parameter names must
     * be unique within a function and must not collide with names declared by
     * {@link #withArguments(String...)}.
     * <p>
     * Example:
     *
     * <pre>
     * element.addJsInitializer(JsFunction.of("setup(target)")
     *         .withParameter("target", someElement));
     * </pre>
     *
     * @param parameterName
     *            the JavaScript identifier under which the value is exposed to
     *            the body, not {@code null}; must not duplicate an existing
     *            parameter or argument name
     * @param value
     *            the value to capture; must be a type supported as a parameter
     *            to {@link Element#executeJs(String, Object...)}
     * @return a new {@code JsFunction} with the additional parameter
     * @throws IllegalArgumentException
     *             if the name conflicts with an existing parameter or argument,
     *             or if the value has a type that cannot be sent to the client
     */
    public JsFunction withParameter(String parameterName,
            @Nullable Object value) {
        Objects.requireNonNull(parameterName, "parameterName");
        if (parameterNames.contains(parameterName)) {
            throw new IllegalArgumentException("Parameter '" + parameterName
                    + "' is already declared on this JsFunction");
        }
        if (argumentNames.contains(parameterName)) {
            throw new IllegalArgumentException("Parameter '" + parameterName
                    + "' conflicts with a runtime argument of the same name");
        }
        // Dry-run encode so unsupported types fail fast, same as of().
        JacksonCodec.encodeWithTypeInfo(value);

        List<@Nullable Object> newCaptures = new ArrayList<>(captures);
        newCaptures.add(value);
        String newBody = "let " + parameterName + "=$" + captures.size() + ";"
                + body;
        List<String> newParameterNames = new ArrayList<>(parameterNames);
        newParameterNames.add(parameterName);
        return new JsFunction(newBody,
                Collections.unmodifiableList(newCaptures), argumentNames,
                Collections.unmodifiableList(newParameterNames));
    }

    /**
     * The JavaScript function body.
     *
     * @return the body string
     */
    public String getBody() {
        return body;
    }

    /**
     * The captured values, in declaration order.
     *
     * @return an unmodifiable list of captures
     */
    public List<@Nullable Object> getCaptures() {
        return captures;
    }

    /**
     * The names of the runtime arguments declared via
     * {@link #withArguments(String...)}, in positional order.
     *
     * @return an unmodifiable list of argument names; empty if none were
     *         declared
     */
    public List<String> getArgumentNames() {
        return argumentNames;
    }
}
