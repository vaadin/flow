/*
 * Copyright 2000-2016 Vaadin Ltd.
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
package com.vaadin.client.hummingbird;

import com.vaadin.client.hummingbird.namespace.MapNamespace;
import com.vaadin.client.hummingbird.namespace.MapProperty;
import com.vaadin.client.hummingbird.reactive.Computation;
import com.vaadin.client.hummingbird.reactive.Reactive;
import com.vaadin.hummingbird.dom.impl.TextNodeNamespace;
import com.vaadin.hummingbird.shared.Namespaces;

import elemental.client.Browser;
import elemental.dom.Text;

/**
 * Binds {@link TextNodeNamespace} data to a text node in the DOM.
 *
 * @since
 * @author Vaadin Ltd
 */
public class TextElementBinder {

    private Computation computation;

    /**
     * Creates a binder for the given state node and DOM text node.
     *
     * @param node
     *            the state node
     * @param text
     *            the DOM text node
     */
    public TextElementBinder(StateNode node, Text text) {
        assert node.hasNamespace(Namespaces.TEXT_NODE);

        MapNamespace textNamespace = node.getMapNamespace(Namespaces.TEXT_NODE);
        MapProperty textProperty = textNamespace.getProperty(Namespaces.TEXT);

        computation = Reactive.runWhenDepedenciesChange(
                () -> text.setData((String) textProperty.getValue()));

        node.addUnregisterListener(e -> remove());

        node.setDomNode(text);
    }

    /**
     * Removes all bindings.
     */
    public final void remove() {
        computation.stop();
    }

    /**
     * Creates a new DOM text node and bind it to the given state node.
     *
     * @param node
     *            the state node to bind to
     * @return the new DOM text node
     */
    public static Text createAndBind(StateNode node) {
        Text text = Browser.getDocument().createTextNode("");

        bind(node, text);

        return text;
    }

    private static TextElementBinder bind(StateNode node, Text text) {
        return new TextElementBinder(node, text);
    }
}
