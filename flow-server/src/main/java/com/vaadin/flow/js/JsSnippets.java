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
 * state, so one implementation can be kept in a constant and shared:
 *
 * <pre>
 * static final LoggerJs LOGGER = JsSnippets.of(LoggerJs.class);
 *
 * button.getElement().executeJs(ClickJs.class)
 *         .onClick(LOGGER.createLogger("Button:"));
 * </pre>
 *
 * Here <code>ClickJs</code> is a definition whose <code>onClick</code> method
 * runs JavaScript that takes a {@link com.vaadin.flow.dom.JsFunction
 * JsFunction} argument, as shown for {@link JsCalledWith}.
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
