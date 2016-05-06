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
import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.stream.Stream;

import com.vaadin.hummingbird.StateNode;
import com.vaadin.hummingbird.dom.ClassList;
import com.vaadin.hummingbird.dom.DomEventListener;
import com.vaadin.hummingbird.dom.Element;
import com.vaadin.hummingbird.dom.ElementStateProvider;
import com.vaadin.hummingbird.dom.EventRegistrationHandle;
import com.vaadin.hummingbird.dom.Style;
import com.vaadin.hummingbird.nodefeature.ComponentMapping;
import com.vaadin.hummingbird.nodefeature.ModelMap;
import com.vaadin.hummingbird.nodefeature.NodeFeature;
import com.vaadin.hummingbird.nodefeature.ParentGeneratorHolder;
import com.vaadin.hummingbird.nodefeature.TemplateMap;
import com.vaadin.hummingbird.nodefeature.TemplateOverridesMap;
import com.vaadin.hummingbird.template.ElementTemplateNode;
import com.vaadin.hummingbird.template.TemplateNode;
import com.vaadin.server.StreamResource;
import com.vaadin.ui.Component;
import com.vaadin.ui.Template;

/**
 * Handles storing and retrieval of the state information for an element defined
 * in a template node.
 *
 * @author Vaadin Ltd
 */
public class TemplateElementStateProvider implements ElementStateProvider {
    @SuppressWarnings("unchecked")
    private static Class<? extends NodeFeature>[] features = new Class[] {
            TemplateMap.class, TemplateOverridesMap.class,
            ComponentMapping.class, ModelMap.class,
            ParentGeneratorHolder.class };

    private static final String CANT_MODIFY_MESSAGE = "Can't modify element defined in a template";
    private ElementTemplateNode templateNode;

    /**
     * Creates a new state provider for the given template definition.
     *
     * @param templateNode
     *            the template definition, not <code>null</code>
     */
    public TemplateElementStateProvider(ElementTemplateNode templateNode) {
        assert templateNode != null;
        this.templateNode = templateNode;
    }

    @Override
    public boolean supports(StateNode node) {
        return Stream.of(features).allMatch(node::hasFeature);
    }

    @Override
    public String getTag(StateNode node) {
        return templateNode.getTag();
    }

    @Override
    public void setAttribute(StateNode node, String attribute, String value) {
        throw new UnsupportedOperationException(CANT_MODIFY_MESSAGE);
    }

    @Override
    public void setAttribute(StateNode node, String attribute,
            StreamResource resource) {
        throw new UnsupportedOperationException(CANT_MODIFY_MESSAGE);
    }

    @Override
    public String getAttribute(StateNode node, String attribute) {
        return templateNode.getAttributeBinding(attribute)
                .map(binding -> binding.getValue(node, ""))
                .map(Object::toString).orElse(null);
    }

    @Override
    public boolean hasAttribute(StateNode node, String attribute) {
        return templateNode.getAttributeBinding(attribute).isPresent();
    }

    @Override
    public void removeAttribute(StateNode node, String attribute) {
        throw new UnsupportedOperationException(CANT_MODIFY_MESSAGE);
    }

    @Override
    public Stream<String> getAttributeNames(StateNode node) {
        return templateNode.getAttributeNames();
    }

    @Override
    public Element getParent(StateNode node) {
        // Implementation shared with template text provider
        return getParent(node, templateNode);
    }

    /**
     * Gets the parent element for a state node and template node.
     *
     * @param node
     *            the state node, not <code>null</code>
     * @param templateNode
     *            the template node, not <code>null</code>
     * @return the element, not <code>null</code>
     */
    public static Element getParent(StateNode node, TemplateNode templateNode) {
        assert node != null;
        assert templateNode != null;

        return templateNode.getParent()
                .map(parent -> parent.getParentElement(node)).orElseGet(
                        () -> BasicElementStateProvider.get().getParent(node));
    }

    @Override
    public int getChildCount(StateNode node) {
        int templateChildCount = templateNode.getChildCount();
        if (templateChildCount == 0) {
            // No children according to the template, could still have an
            // override node with children
            Optional<StateNode> overrideNode = getOverrideNode(node);
            if (overrideNode.isPresent()) {
                return BasicElementStateProvider.get()
                        .getChildCount(overrideNode.get());
            }
        }

        int elementChildCount = 0;
        for (int i = 0; i < templateChildCount; i++) {
            TemplateNode templateChild = templateNode.getChild(i);
            elementChildCount += templateChild.getGeneratedElementCount(node);
        }
        return elementChildCount;
    }

    @Override
    public Element getChild(StateNode node, int index) {
        int templateChildCount = templateNode.getChildCount();
        if (templateChildCount == 0) {
            // No children according to the template, could still have an
            // override node with children
            Optional<StateNode> overrideNode = getOverrideNode(node);
            if (overrideNode.isPresent()) {
                return BasicElementStateProvider.get()
                        .getChild(overrideNode.get(), index);
            }
        }

        int currentChildIndex = 0;
        for (int i = 0; i < templateChildCount; i++) {
            TemplateNode templateChild = templateNode.getChild(i);
            int generateCount = templateChild.getGeneratedElementCount(node);

            int indexInChild = index - currentChildIndex;
            if (indexInChild < generateCount) {
                return templateChild.getElement(indexInChild, node);
            }

            currentChildIndex += generateCount;
        }

        throw new IndexOutOfBoundsException();
    }

    @Override
    public void insertChild(StateNode node, int index, Element child) {
        modifyChildren(node, (provider, overrideNode) -> provider
                .insertChild(overrideNode, index, child));
    }

    @Override
    public void removeChild(StateNode node, int index) {
        modifyChildren(node, (provider, overrideNode) -> provider
                .removeChild(overrideNode, index));
    }

    @Override
    public void removeChild(StateNode node, Element child) {
        modifyChildren(node, (provider, overrideNode) -> provider
                .removeChild(overrideNode, child));
    }

    @Override
    public void removeAllChildren(StateNode node) {
        modifyChildren(node, (provider, overrideNode) -> provider
                .removeAllChildren(overrideNode));
    }

    private void modifyChildren(StateNode node,
            BiConsumer<BasicElementStateProvider, StateNode> modifier) {
        if (templateNode.getChildCount() != 0) {
            throw new IllegalStateException(
                    "Can't add or remove children when there are children defined by the template.");
        }

        StateNode overrideNode = getOrCreateOverrideNode(node);

        modifier.accept(BasicElementStateProvider.get(), overrideNode);
    }

    @Override
    public EventRegistrationHandle addEventListener(StateNode node,
            String eventType, DomEventListener listener,
            String[] eventDataExpressions) {
        throw new UnsupportedOperationException(CANT_MODIFY_MESSAGE);
    }

    @Override
    public Object getProperty(StateNode node, String name) {
        return templateNode.getPropertyBinding(name)
                .map(binding -> binding.getValue(node, "")).orElse(null);
    }

    @Override
    public void setProperty(StateNode node, String name, Serializable value,
            boolean emitChange) {
        throw new UnsupportedOperationException(CANT_MODIFY_MESSAGE);
    }

    @Override
    public void removeProperty(StateNode node, String name) {
        throw new UnsupportedOperationException(CANT_MODIFY_MESSAGE);
    }

    @Override
    public boolean hasProperty(StateNode node, String name) {
        return templateNode.getPropertyBinding(name).isPresent();
    }

    @Override
    public Stream<String> getPropertyNames(StateNode node) {
        return templateNode.getPropertyNames();
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
        // Should eventually be based on [class.foo]=bar in the template
        return new ImmutableEmptyClassList();
    }

    @Override
    public Style getStyle(StateNode node) {
        // Should eventually be based on [style.foo]=bar in the template
        return new ImmutableEmptyStyle();
    }

    @Override
    public Set<String> getSynchronizedProperties(StateNode node) {
        return Collections.emptySet();
    }

    @Override
    public Set<String> getSynchronizedPropertyEvents(StateNode node) {
        return Collections.emptySet();
    }

    @Override
    public void setComponent(StateNode node, Component component) {
        assert node != null;
        assert component != null;

        if (!(component instanceof Template)) {
            throw new IllegalArgumentException(
                    "Component for template element must extend "
                            + Template.class.getName());
        }

        node.getFeature(ComponentMapping.class).setComponent(component);
    }

    @Override
    public Optional<Component> getComponent(StateNode node) {
        return Optional.empty();
    }

    private Optional<StateNode> getOverrideNode(StateNode node) {
        return Optional.ofNullable(node.getFeature(TemplateOverridesMap.class)
                .get(templateNode, false));
    }

    private StateNode getOrCreateOverrideNode(StateNode node) {
        return node.getFeature(TemplateOverridesMap.class).get(templateNode,
                true);
    }

    /**
     * Creates a new state node with all features needed for a template.
     *
     * @return a new state node, not <code>null</code>
     */
    public static StateNode createNode() {
        return new StateNode(features);
    }
}
