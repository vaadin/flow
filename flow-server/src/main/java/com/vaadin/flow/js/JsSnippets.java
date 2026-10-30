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
package com.vaadin.flow.js;

import java.util.Objects;

import com.vaadin.flow.dom.Element;

/**
 * Hands out implementations of {@link JsDefinition} interfaces whose methods
 * declare {@link JsSnippet snippets}: JavaScript that is not run when the
 * method is called, but evaluated later on the client by what the returned
 * value is passed to.
 * <p>
 * A snippet is not bound to an element, which is why the implementation comes
 * from here rather than from {@link Element#executeJs(Class)}. It holds no
 * state, so one implementation can be kept in a constant and shared.
 * <p>
 * A snippet declares a function that a call of another definition then gets as
 * an argument:
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
 * &#64;JsDefinition
 * public interface ClickJs extends Serializable {
 *
 *     &#64;JsExpression("this.addEventListener('click', () =&gt; $0('clicked'));")
 *     void onClick(JsFunction handler);
 * }
 *
 * static final LoggerJs LOGGER = JsSnippets.of(LoggerJs.class);
 *
 * button.getElement().executeJs(ClickJs.class)
 *         .onClick(LOGGER.createLogger("Button:"));
 * </pre>
 *
 * The build collects both declarations into the bundle, roughly as:
 *
 * <pre>
 * // LoggerJs.createLogger: a snippet, evaluated with the captured arguments
 * jsSnippets["&lt;id&gt;"] = function ($0) {
 *     return function (value) {
 *         console.log($0, value);
 *     };
 * };
 * // ClickJs.onClick: a call, run on the element it was obtained from
 * jsDefinitions["&lt;id&gt;"] = async function ($0) {
 *     this.addEventListener('click', () =&gt; $0('clicked'));
 * };
 * </pre>
 *
 * The server sends the browser only the two identifiers and the argument
 * <code>"Button:"</code>. The client calls the snippet function with
 * <code>"Button:"</code> to get the logger, and runs <code>onClick</code> on
 * the button with that logger as <code>$0</code>, so a click logs
 * <code>Button: clicked</code>. Nothing is compiled from a string, which is
 * what keeps this working under a content security policy that does not allow
 * <code>unsafe-eval</code>.
 *
 * @see JsSnippet
 * @see JsCalledWith
 */
public final class JsSnippets {

    private JsSnippets() {
        // Only static members
    }

    /**
     * Gets an implementation of the given JavaScript definition, whose methods
     * answer with the snippet they declare.
     *
     * @param <T>
     *            the JavaScript definition type
     * @param definitionType
     *            the JavaScript definition, not <code>null</code>
     * @return an implementation of the interface, not <code>null</code>
     * @throws IllegalArgumentException
     *             if the type is not an interface, is not annotated with
     *             {@link JsDefinition}, or has a method that does not declare a
     *             snippet
     */
    public static <T> T of(Class<T> definitionType) {
        Objects.requireNonNull(definitionType,
                "Definition type cannot be null");
        return JsDefinitionProxy.createSnippets(definitionType);
    }
}
