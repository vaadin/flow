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
package com.vaadin.flow.dom.impl;

import java.io.Serializable;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Stream;

import com.vaadin.flow.dom.ChildElementConsumer;
import com.vaadin.flow.dom.ClassList;
import com.vaadin.flow.dom.DisabledUpdateMode;
import com.vaadin.flow.dom.DomEventListener;
import com.vaadin.flow.dom.DomListenerRegistration;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.dom.ElementStateProvider;
import com.vaadin.flow.dom.NodeVisitor;
import com.vaadin.flow.dom.NodeVisitor.ElementType;
import com.vaadin.flow.dom.PropertyChangeListener;
import com.vaadin.flow.dom.Style;
import com.vaadin.flow.internal.StateNode;
import com.vaadin.flow.server.AbstractStreamResource;
import com.vaadin.flow.shared.Registration;

/**
 * Abstract element state provider for text nodes. Operations that are not
 * applicable for text nodes throw {@link UnsupportedOperationException}.
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
public abstract class AbstractTextElementStateProvider
        implements ElementStateProvider {

    @Override
    public boolean isTextNode(StateNode node) {
        return true;
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
        return null;
    }

    @Override
    public boolean hasAttribute(StateNode node, String attribute) {
        return false;
    }

    @Override
    public void removeAttribute(StateNode node, String attribute) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Stream<String> getAttributeNames(StateNode node) {
        return Stream.empty();
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
    public DomListenerRegistration addEventListener(StateNode node,
            String eventType, DomEventListener listener) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Serializable getProperty(StateNode node, String name) {
        return null;
    }

    @Override
    public void setProperty(StateNode node, String name, Serializable value,
            boolean emitChange) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void removeProperty(StateNode node, String name) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean hasProperty(StateNode node, String name) {
        return false;
    }

    @Override
    public Stream<String> getPropertyNames(StateNode node) {
        return Stream.empty();
    }

    @Override
    public ClassList getClassList(StateNode node) {
        return new ImmutableClassList(Collections.emptyList());
    }

    @Override
    public Style getStyle(StateNode node) {
        return new ImmutableEmptyStyle();
    }

    @Override
    public Set<String> getSynchronizedProperties(StateNode node) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Set<String> getSynchronizedPropertyEvents(StateNode node) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setAttribute(StateNode node, String attribute,
            AbstractStreamResource resource) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Registration addPropertyChangeListener(StateNode node, String name,
            PropertyChangeListener listener) {
        throw new UnsupportedOperationException();
    }

    @Override
    public StateNode getShadowRoot(StateNode node) {
        return null;
    }

    @Override
    public StateNode attachShadow(StateNode node) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void attachExistingElement(StateNode node, String tagName,
            Element previousSibling, ChildElementConsumer callback) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void appendVirtualChild(StateNode node, Element child, String type,
            String payload) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void visit(StateNode node, NodeVisitor visitor) {
        visitor.visit(ElementType.REGULAR, Element.get(node));
    }

    @Override
    public void setVisible(StateNode node, boolean visible) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isVisible(StateNode node) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void addSynchronizedProperty(StateNode node, String property,
            DisabledUpdateMode mode) {
        throw new UnsupportedOperationException();
    }
}
