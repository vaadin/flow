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

import java.io.Serializable;
import java.util.Set;

import com.vaadin.hummingbird.StateNode;
import com.vaadin.hummingbird.dom.DomEventListener;
import com.vaadin.hummingbird.dom.Element;
import com.vaadin.hummingbird.dom.ElementStateProvider;
import com.vaadin.hummingbird.dom.EventRegistrationHandle;
import com.vaadin.hummingbird.dom.Style;

import elemental.json.JsonValue;

/**
 * Handles storing and retrieval of the state information for a text node using
 * a state node.
 *
 * @since
 * @author Vaadin Ltd
 */
public class TextElementStateProvider implements ElementStateProvider {
    private static final TextElementStateProvider INSTANCE = new TextElementStateProvider();

    private TextElementStateProvider() {
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
    public static TextElementStateProvider get() {
        return INSTANCE;
    }

    @Override
    public boolean supports(StateNode node) {
        return node.hasNamespace(TextNodeNamespace.class);
    }

    @Override
    public String getTag(StateNode node) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setAttribute(StateNode node, String attribute, String value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getAttribute(StateNode node, String attribute) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean hasAttribute(StateNode node, String attribute) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void removeAttribute(StateNode node, String attribute) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Set<String> getAttributeNames(StateNode node) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Element getParent(StateNode node) {
        return BasicElementStateProvider.get().getParent(node);
    }

    @Override
    public int getChildCount(StateNode node) {
        return 0;
    }

    @Override
    public Element getChild(StateNode node, int index) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void insertChild(StateNode node, int index, Element child) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void removeChild(StateNode node, int index) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void removeChild(StateNode node, Element child) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void removeAllChildren(StateNode node) {
        throw new UnsupportedOperationException();
    }

    @Override
    public EventRegistrationHandle addEventListener(StateNode node,
            String eventType, DomEventListener listener, String[] eventDataExpressions) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object getProperty(StateNode node, String name) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setProperty(StateNode node, String name, Serializable value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setJsonProperty(StateNode node, String name, JsonValue value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void removeProperty(StateNode node, String name) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean hasProperty(StateNode node, String name) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Set<String> getPropertyNames(StateNode node) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isTextNode(StateNode node) {
        return true;
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

    @Override
    public Set<String> getClassList(StateNode node) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Style getStyle(StateNode node) {
        throw new UnsupportedOperationException();
    }
}
