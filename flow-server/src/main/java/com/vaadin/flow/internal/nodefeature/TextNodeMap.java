/*
 * Copyright 2000-2018 Vaadin Ltd.
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
package com.vaadin.flow.internal.nodefeature;

import com.vaadin.flow.internal.StateNode;

/**
 * Map holding the data of a text node.
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
public class TextNodeMap extends NodeValue<String> {

    /**
     * Creates a new text node map for the given node.
     *
     * @param node
     *            the node that the map belongs to
     */
    public TextNodeMap(StateNode node) {
        super(node);
    }

    @Override
    protected String getKey() {
        return NodeProperties.TEXT;
    }

    /**
     * Sets the text of this node.
     *
     * @param text
     *            the text, not <code>null</code>
     */
    public void setText(String text) {
        assert text != null;

        setValue(text);
    }

    /**
     * Gets the text of this node.
     *
     * @return the text, not null
     */
    public String getText() {
        String value = getValue();

        // Text should be set upon creation, before first use
        assert value != null;

        return value;
    }

}
