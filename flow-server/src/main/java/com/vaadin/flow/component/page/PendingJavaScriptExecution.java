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
package com.vaadin.flow.component.page;

import org.jspecify.annotations.Nullable;

import com.vaadin.flow.dom.Element;

/**
 * A pending {@link Element#executeJs(String, Object...) executeJs} result that
 * additionally supports adding named parameters to the JavaScript expression.
 * <p>
 * Named parameters are an alternative to the positional {@code $0}, {@code $1},
 * &hellip; placeholders: the value is made available to the expression as a
 * variable with the chosen name, so the expression can read
 * {@code doSomething(foo)} instead of {@code doSomething($0)}. Positional
 * parameters passed to {@code executeJs} and named parameters added via
 * {@link #withParameter(String, Object)} can be combined freely.
 * <p>
 * Parameters can only be added before the execution has been sent to the
 * browser. Adding a parameter after {@link #isSentToBrowser()} returns
 * {@code true} throws an {@link IllegalStateException}.
 *
 * @author Vaadin Ltd
 */
public interface PendingJavaScriptExecution extends PendingJavaScriptResult {

    /**
     * Adds a named parameter that the JavaScript expression can reference
     * directly by name. The value is converted to JavaScript using the same
     * rules as the positional parameters of
     * {@link Element#executeJs(String, Object...) executeJs}.
     * <p>
     * Example:
     *
     * <pre>
     * element.executeJs("doSomething(foo)").withParameter("foo", "Some value");
     * </pre>
     *
     * @param name
     *            the name to expose the value as in the JavaScript expression;
     *            must be a valid JavaScript identifier and must not start with
     *            {@code $}
     * @param value
     *            the value to pass; supports the same types as the positional
     *            parameters of {@link Element#executeJs(String, Object...)}
     * @return this pending execution, for chaining
     * @throws IllegalArgumentException
     *             if the name is not a valid identifier, has already been
     *             registered, or the value has an unsupported type
     * @throws IllegalStateException
     *             if the execution has already been sent to the browser
     */
    PendingJavaScriptExecution withParameter(String name,
            @Nullable Object value);
}
