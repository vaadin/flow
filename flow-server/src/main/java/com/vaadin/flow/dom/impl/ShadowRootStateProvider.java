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
import java.util.Set;
import java.util.stream.Stream;

import com.vaadin.flow.dom.ClassList;
import com.vaadin.flow.dom.DisabledUpdateMode;
import com.vaadin.flow.dom.DomEventListener;
import com.vaadin.flow.dom.DomListenerRegistration;
import com.vaadin.flow.dom.Node;
import com.vaadin.flow.dom.NodeVisitor;
import com.vaadin.flow.dom.PropertyChangeListener;
import com.vaadin.flow.dom.ShadowRoot;
import com.vaadin.flow.dom.Style;
import com.vaadin.flow.internal.StateNode;
import com.vaadin.flow.internal.nodefeature.AttachExistingElementFeature;
import com.vaadin.flow.internal.nodefeature.ElementChildrenList;
import com.vaadin.flow.internal.nodefeature.NodeFeature;
import com.vaadin.flow.internal.nodefeature.ReturnChannelMap;
import com.vaadin.flow.internal.nodefeature.ShadowRootData;
import com.vaadin.flow.internal.nodefeature.ShadowRootHost;
import com.vaadin.flow.internal.nodefeature.VirtualChildrenList;
import com.vaadin.flow.server.AbstractStreamResource;
import com.vaadin.flow.shared.Registration;

/**
 * Implementation which handles shadow root nodes.
 * <p>
 * Only the methods implemented in the {@link AbstractNodeStateProvider} are
 * supported (related to the composition).
 * <p>
 * The data is stored directly in the state node but this should be considered
 * an implementation detail which can change.
 *
 * @author Vaadin Ltd
 * @since 1.0
 *
 */
public class ShadowRootStateProvider extends AbstractNodeStateProvider {

    private static final ShadowRootStateProvider INSTANCE = new ShadowRootStateProvider();

    @SuppressWarnings("unchecked")
    private static final Class<? extends NodeFeature>[] FEATURES = new Class[] {
            ElementChildrenList.class, ShadowRootHost.class,
            AttachExistingElementFeature.class, VirtualChildrenList.class,
            ReturnChannelMap.class };

    /**
     * Gets the one and only instance.
     *
     * @return the instance to use for shadow root nodes
     */
    public static ShadowRootStateProvider get() {
        return INSTANCE;
    }

    /**
     * Create a new shadow root node for the given element {@code node}.
     *
     * @param node
     *            the node to create the shadow root for
     * @return the shadow root node
     */
    public StateNode createShadowRootNode(StateNode node) {
        StateNode shadowRoot = new StateNode(getProviderFeatures());
        node.getFeature(ShadowRootData.class).setShadowRoot(shadowRoot);
        return shadowRoot;
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
    public void setAttribute(StateNode node, String attribute,
            AbstractStreamResource resource) {
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
    public Stream<String> getAttributeNames(StateNode node) {
        throw new UnsupportedOperationException();
    }

    @Override
    public DomListenerRegistration addEventListener(StateNode node,
            String eventType, DomEventListener listener) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Serializable getProperty(StateNode node, String name) {
        throw new UnsupportedOperationException();
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
        throw new UnsupportedOperationException();
    }

    @Override
    public Stream<String> getPropertyNames(StateNode node) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isTextNode(StateNode node) {
        return false;
    }

    @Override
    public String getTextContent(StateNode node) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setTextContent(StateNode node, String textContent) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ClassList getClassList(StateNode node) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Style getStyle(StateNode node) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Set<String> getSynchronizedProperties(StateNode node) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void addSynchronizedProperty(StateNode node, String property,
            DisabledUpdateMode mode) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Set<String> getSynchronizedPropertyEvents(StateNode node) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Registration addPropertyChangeListener(StateNode node, String name,
            PropertyChangeListener listener) {
        throw new UnsupportedOperationException();
    }

    @Override
    public StateNode getShadowRoot(StateNode node) {
        throw new UnsupportedOperationException();
    }

    @Override
    public StateNode attachShadow(StateNode node) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void visit(StateNode node, NodeVisitor visitor) {
        ShadowRoot shadowRoot = ShadowRoot.get(node);
        boolean visitDescendants = visitor.visit(shadowRoot);

        if (visitDescendants) {
            visitDescendants(shadowRoot, visitor);
        }
    }

    @Override
    protected Class<? extends NodeFeature>[] getProviderFeatures() {
        return FEATURES;
    }

    @Override
    protected Node<?> getNode(StateNode node) {
        assert supports(node);
        return ShadowRoot.get(node);
    }

    @Override
    public void setVisible(StateNode node, boolean visible) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isVisible(StateNode node) {
        throw new UnsupportedOperationException();
    }
}
