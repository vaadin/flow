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

import java.io.Serializable;
import java.util.Objects;

import com.vaadin.flow.dom.Element;

/**
 * Marks an interface whose methods declare {@link JsSnippet snippets}:
 * JavaScript that is not run when the method is called, but evaluated later on
 * the client by what the returned value is passed to. An implementation is
 * obtained from {@link #of(Class)}.
 * <p>
 * Like with {@link JsInvokers}, an interface that is passed to
 * {@link #of(Class)} must itself be annotated with {@link JsDefinition}, which
 * is what makes it findable during the build. Annotations on interfaces are not
 * inherited. An interface can not extend both marker interfaces. See
 * {@link JsDefinition} for how a definition that extends another interface is
 * declared.
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
 * public interface LoggerJs extends JsSnippets {
 *
 *     &#64;JsExpression("console.log($0, value);")
 *     &#64;JsFunctionParameterNames("value")
 *     JsFunction createLogger(String prefix);
 * }
 *
 * &#64;JsDefinition
 * public interface ClickJs extends JsInvokers {
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
 * @see JsFunctionParameterNames
 * @since 25.4
 */
public interface JsSnippets extends Serializable {

    /**
     * Gets an implementation of the given interface, whose methods answer with
     * the snippet they declare.
     *
     * @param <T>
     *            the type of the interface
     * @param definitionType
     *            the interface that declares the snippets, annotated with
     *            {@link JsDefinition}, not <code>null</code>
     * @return an implementation of the interface, not <code>null</code>
     * @throws IllegalArgumentException
     *             if the type is not an interface, is not annotated with
     *             {@link JsDefinition}, also extends {@link JsInvokers}, or has
     *             a method that does not declare a snippet
     */
    static <T extends JsSnippets> T of(Class<T> definitionType) {
        Objects.requireNonNull(definitionType,
                "Definition type cannot be null");
        return JsDefinitionProxy.createSnippets(definitionType);
    }
}
