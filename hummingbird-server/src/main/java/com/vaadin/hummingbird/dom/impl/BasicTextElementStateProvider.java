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

/**
 * Handles storing and retrieval of the state information for a text node using
 * a state node.
 *
 * @since
 * @author Vaadin Ltd
 */
public class BasicTextElementStateProvider extends AbstractTextElementStateProvider {
    private static final BasicTextElementStateProvider INSTANCE = new BasicTextElementStateProvider();

    private BasicTextElementStateProvider() {
        // Singleton
    }

    /**
     * Creates a compatible text state node using the given text.
     *
     * @param text
     *            the text to use
     * @return a initialized and compatible state node
     */
    public static StateNode createStateNode(String text) {
        assert text != null;

        StateNode node = new StateNode(TextNodeNamespace.class);
        node.getNamespace(TextNodeNamespace.class).setText(text);

        return node;
    }

    /**
     * Gets the one and only instance.
     *
     * @return the instance to use for all basic text nodes
     */
    public static BasicTextElementStateProvider get() {
        return INSTANCE;
    }

    @Override
    public boolean supports(StateNode node) {
        return node.hasNamespace(TextNodeNamespace.class);
    }

    @Override
    public String getTextContent(StateNode node) {
        assert node != null;

        return node.getNamespace(TextNodeNamespace.class).getText();
    }

    @Override
    public void setTextContent(StateNode node, String textContent) {
        assert node != null;
        assert textContent != null;

        node.getNamespace(TextNodeNamespace.class).setText(textContent);
    }
}
