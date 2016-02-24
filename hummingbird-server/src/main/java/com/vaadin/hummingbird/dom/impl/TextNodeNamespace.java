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
package com.vaadin.hummingbird.dom.impl;

import com.vaadin.hummingbird.StateNode;
import com.vaadin.hummingbird.namespace.MapNamespace;
import com.vaadin.hummingbird.shared.Namespaces;

/**
 * Namespace holding the data of a text node.
 *
 * @since
 * @author Vaadin Ltd
 */
public class TextNodeNamespace extends MapNamespace {

    /**
     * Creates a new text node namespace for the given node.
     *
     * @param node
     */
    public TextNodeNamespace(StateNode node) {
        super(node);
    }

    /**
     * Sets the text of this node.
     *
     * @param text
     *            the text, not null
     */
    public void setText(String text) {
        assert text != null;

        put(Namespaces.TEXT, text,true);
    }

    /**
     * Gets the text of this node.
     *
     * @return the text, not null
     */
    public String getText() {
        // Text should be set upon creation, before first use
        assert contains(Namespaces.TEXT);

        return (String) get(Namespaces.TEXT);
    }
}
