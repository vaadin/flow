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

import java.util.Optional;

import com.vaadin.hummingbird.StateNode;
import com.vaadin.hummingbird.dom.Element;
import com.vaadin.hummingbird.nodefeature.ComponentMapping;
import com.vaadin.hummingbird.nodefeature.ParentGeneratorHolder;
import com.vaadin.hummingbird.nodefeature.TextNodeMap;
import com.vaadin.ui.Component;

/**
 * Handles storing and retrieval of the state information for a text node using
 * a state node.
 *
 * @author Vaadin Ltd
 */
public class BasicTextElementStateProvider
        extends AbstractTextElementStateProvider {
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

        StateNode node = new StateNode(TextNodeMap.class,
                ComponentMapping.class, ParentGeneratorHolder.class);
        node.getFeature(TextNodeMap.class).setText(text);

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
        return node.hasFeature(TextNodeMap.class);
    }

    @Override
    public String getTextContent(StateNode node) {
        assert node != null;

        return node.getFeature(TextNodeMap.class).getText();
    }

    @Override
    public void setTextContent(StateNode node, String textContent) {
        assert node != null;
        assert textContent != null;

        node.getFeature(TextNodeMap.class).setText(textContent);
    }

    @Override
    public Element getParent(StateNode node) {
        return BasicElementStateProvider.get().getParent(node);
    }

    @Override
    public void setComponent(StateNode node, Component component) {
        assert node != null;
        assert component != null;
        node.getFeature(ComponentMapping.class).setComponent(component);
    }

    @Override
    public Optional<Component> getComponent(StateNode node) {
        assert node != null;
        return node.getFeature(ComponentMapping.class).getComponent();
    }

}
