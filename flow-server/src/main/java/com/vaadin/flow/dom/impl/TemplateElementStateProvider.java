/*
 * Copyright 2000-2017 Vaadin Ltd.
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
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Predicate;
import java.util.stream.Stream;

import com.vaadin.flow.StateNode;
import com.vaadin.flow.dom.ChildElementConsumer;
import com.vaadin.flow.dom.ClassList;
import com.vaadin.flow.dom.DomEventListener;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.dom.ElementStateProvider;
import com.vaadin.flow.dom.Node;
import com.vaadin.flow.dom.Style;
import com.vaadin.flow.nodefeature.ClientDelegateHandlers;
import com.vaadin.flow.nodefeature.ComponentMapping;
import com.vaadin.flow.nodefeature.ElementAttributeMap;
import com.vaadin.flow.nodefeature.ElementChildrenList;
import com.vaadin.flow.nodefeature.ElementListenerMap;
import com.vaadin.flow.nodefeature.ElementPropertyMap;
import com.vaadin.flow.nodefeature.ModelList;
import com.vaadin.flow.nodefeature.ModelMap;
import com.vaadin.flow.nodefeature.NodeFeature;
import com.vaadin.flow.nodefeature.OverrideElementData;
import com.vaadin.flow.nodefeature.ParentGeneratorHolder;
import com.vaadin.flow.nodefeature.SynchronizedPropertiesList;
import com.vaadin.flow.nodefeature.SynchronizedPropertyEventsList;
import com.vaadin.flow.nodefeature.TemplateMap;
import com.vaadin.flow.nodefeature.TemplateOverridesMap;
import com.vaadin.flow.template.angular.BindingValueProvider;
import com.vaadin.flow.template.angular.ElementTemplateNode;
import com.vaadin.flow.template.angular.StaticBindingValueProvider;
import com.vaadin.flow.template.angular.TemplateNode;
import com.vaadin.server.AbstractStreamResource;
import com.vaadin.shared.Registration;
import com.vaadin.ui.AngularTemplate;
import com.vaadin.ui.Component;
import com.vaadin.ui.event.PropertyChangeListener;

/**
 * Handles storing and retrieval of the state information for an element defined
 * in a template node.
 *
 * @author Vaadin Ltd
 */
public class TemplateElementStateProvider implements ElementStateProvider {

    // Node features needed for a state node that represents the root of a
    // template
    @SuppressWarnings("unchecked")
    private static Class<? extends NodeFeature>[] rootNodeFeatures = new Class[] {
            ComponentMapping.class, TemplateMap.class,
            ParentGeneratorHolder.class, ClientDelegateHandlers.class,
            TemplateOverridesMap.class, ModelMap.class };

    private static Class<? extends NodeFeature>[] overrideNodeFeatures = Stream
            .of(OverrideElementData.class, ElementChildrenList.class,
                    ParentGeneratorHolder.class, ComponentMapping.class,
                    ElementAttributeMap.class, ElementPropertyMap.class,
                    ElementListenerMap.class, SynchronizedPropertiesList.class,
                    SynchronizedPropertyEventsList.class,
                    ClientDelegateHandlers.class)
            .toArray(Class[]::new);

    private static final Predicate<? super String> excludeCustomAttributes = name -> !CustomAttribute
            .getNames().contains(name);

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

    /**
     * Gets the template node for this state provider.
     *
     * @return the template node
     */
    public ElementTemplateNode getTemplateNode() {
        return templateNode;
    }

    @Override
    public boolean supports(StateNode node) {
        return node.hasFeature(TemplateOverridesMap.class);
    }

    @Override
    public String getTag(StateNode node) {
        return templateNode.getTag();
    }

    @Override
    public void setAttribute(StateNode node, String attribute, String value) {
        checkModifiableAttribute(attribute);
        modifyOverrideNode(node, (provider, overrideNode) -> provider
                .setAttribute(overrideNode, attribute, value));
    }

    @Override
    public void setAttribute(StateNode node, String attribute,
            AbstractStreamResource resource) {
        checkModifiableAttribute(attribute);
        modifyOverrideNode(node, (provider, overrideNode) -> provider
                .setAttribute(overrideNode, attribute, resource));
    }

    @Override
    public String getAttribute(StateNode node, String attribute) {
        Optional<BindingValueProvider> provider = templateNode
                .getAttributeBinding(attribute);
        String boundValue = provider.map(binding -> binding.getValue(node, ""))
                .orElse(null);
        if (provider.isPresent()
                && !(provider.get() instanceof StaticBindingValueProvider)) {
            return boundValue;
        }
        /*
         * For non-static bindings always fetch an attribute from the override
         * node first if it exists. In contrast with properties attributes may
         * be defined in the template inlined. But this definition may be
         * overridden and overridden value should be used if any.
         *
         * All static bindings attributes have been copied to override node at
         * its creation time.
         */
        Optional<StateNode> overrideNode = getOverrideNode(node);
        if (overrideNode.isPresent()) {
            return BasicElementStateProvider.get()
                    .getAttribute(overrideNode.get(), attribute);
        }
        return boundValue;
    }

    @Override
    public boolean hasAttribute(StateNode node, String attribute) {
        Optional<StateNode> overrideNode = getOverrideNode(node);
        Optional<BindingValueProvider> provider = templateNode
                .getAttributeBinding(attribute);
        if (provider.isPresent()
                && !(provider.get() instanceof StaticBindingValueProvider)) {
            return true;
        }
        /*
         * For non-static bindings always check an attribute from the override
         * node first if it exists. In contrast with properties attributes may
         * be defined in the template inlined. But this definition may be
         * overridden and overridden value should be used if any.
         *
         * All static bindings attributes have been copied to override node at
         * its creation time.
         */
        if (overrideNode.isPresent()) {
            return BasicElementStateProvider.get()
                    .hasAttribute(overrideNode.get(), attribute);
        }
        return provider.isPresent();
    }

    @Override
    public void removeAttribute(StateNode node, String attribute) {
        checkModifiableAttribute(attribute);
        modifyOverrideNode(node, (provider, overrideNode) -> provider
                .removeAttribute(overrideNode, attribute));
    }

    @Override
    public Stream<String> getAttributeNames(StateNode node) {
        // Exclude custom attributes since those are included on demand by
        // Element.getAttributeNames().
        Stream<String> templateAttributes = templateNode.getAttributeNames()
                .filter(excludeCustomAttributes);

        Optional<StateNode> overrideNode = getOverrideNode(node);
        if (overrideNode.isPresent()) {
            Predicate<String> isStaticBinding = this::isStaticBindingAttribute;
            return Stream.concat(
                    templateAttributes.filter(isStaticBinding.negate()),
                    BasicElementStateProvider.get()
                            .getAttributeNames(overrideNode.get()));
        } else {
            return templateAttributes;
        }
    }

    @Override
    public Node getParent(StateNode node) {
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
    @SuppressWarnings("rawtypes")
    public static Node getParent(StateNode node, TemplateNode templateNode) {
        assert node != null;
        assert templateNode != null;

        return templateNode.getParent()
                .map(parent -> (Node) parent.getParentElement(node)).orElseGet(
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

    @Override
    public Registration addEventListener(StateNode node, String eventType,
            DomEventListener listener, String[] eventDataExpressions) {
        ElementListenerMap listeners = getOrCreateOverrideNode(node)
                .getFeature(ElementListenerMap.class);

        return listeners.add(eventType, listener, eventDataExpressions);
    }

    @Override
    public Serializable getProperty(StateNode node, String name) {
        if (templateNode.getPropertyBinding(name).isPresent()) {
            return (Serializable) templateNode.getPropertyBinding(name).get()
                    .getValue(node);
        } else {
            return getOverrideNode(node)
                    .map(overrideNode -> BasicElementStateProvider.get()
                            .getProperty(overrideNode, name))
                    .orElse(null);
        }
    }

    @Override
    public void setProperty(StateNode node, String name, Serializable value,
            boolean emitChange) {
        checkModifiableProperty(name);
        modifyOverrideNode(node, (provider, overrideNode) -> provider
                .setProperty(overrideNode, name, value, emitChange));
    }

    @Override
    public void removeProperty(StateNode node, String name) {
        checkModifiableProperty(name);
        modifyOverrideNode(node, (provider, overrideNode) -> provider
                .removeProperty(overrideNode, name));
    }

    @Override
    public boolean hasProperty(StateNode node, String name) {
        return templateNode.getPropertyBinding(name).isPresent()
                || getOverrideNode(node)
                        .map(overrideNode -> BasicElementStateProvider.get()
                                .hasProperty(overrideNode, name))
                        .orElse(false);
    }

    @Override
    public Stream<String> getPropertyNames(StateNode node) {
        Stream<String> regularProperties = getOverrideNode(node)
                .map(BasicElementStateProvider.get()::getPropertyNames)
                .orElse(Stream.empty());
        return Stream.concat(templateNode.getPropertyNames(),
                regularProperties);
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
        return new BoundClassList(templateNode, node);
    }

    @Override
    public Style getStyle(StateNode node) {
        return new BoundStyle(templateNode, node);
    }

    @Override
    public Set<String> getSynchronizedProperties(StateNode node) {
        return getOrCreateOverrideNode(node)
                .getFeature(SynchronizedPropertiesList.class)
                .getSynchronizedProperties();
    }

    @Override
    public Set<String> getSynchronizedPropertyEvents(StateNode node) {
        return getOrCreateOverrideNode(node)
                .getFeature(SynchronizedPropertyEventsList.class)
                .getSynchronizedPropertyEvents();
    }

    @Override
    public Optional<Component> getComponent(StateNode node) {
        assert node != null;

        if (isTemplateRoot()) {
            return ElementStateProvider.super.getComponent(node);
        } else {
            Optional<StateNode> overrideNode = getOverrideNode(node);
            return overrideNode.flatMap(
                    n -> n.getFeature(ComponentMapping.class).getComponent());
        }
    }

    private boolean isTemplateRoot() {
        return !templateNode.getParent().isPresent();
    }

    @Override
    public void setComponent(StateNode node, Component component) {
        assert node != null;
        assert component != null;

        if (isTemplateRoot()) {
            if (!(component instanceof AngularTemplate)) {
                throw new IllegalArgumentException(
                        "The component for a template root must extend "
                                + AngularTemplate.class.getName());
            }
            ElementStateProvider.super.setComponent(node, component);
        } else {
            getOrCreateOverrideNode(node).getFeature(ComponentMapping.class)
                    .setComponent(component);
        }

    }

    private Optional<StateNode> getOverrideNode(StateNode node) {
        return Optional.ofNullable(node.getFeature(TemplateOverridesMap.class)
                .get(templateNode, false));
    }

    private StateNode getOrCreateOverrideNode(StateNode node) {
        Optional<StateNode> stateNode = getOverrideNode(node);
        if (!stateNode.isPresent()) {
            stateNode = Optional.of(node.getFeature(TemplateOverridesMap.class)
                    .get(templateNode, true));
            StateNode overrideNode = stateNode.get();
            /*
             * Transfer all static attribute binding values as initial values to
             * override node. It allows to get attribute values from overridden
             * node ONLY once it's created (and don't ask StaticBinding value).
             * If it's not created then StaticBinding value is used as an
             * attribute value. This approach allows to track removed attributes
             * (since such attribute will always be asked from override node).
             */
            templateNode.getAttributeNames()
                    .filter(this::isStaticBindingAttribute)
                    .filter(excludeCustomAttributes)
                    .forEach(attribute -> BasicElementStateProvider.get()
                            .setAttribute(overrideNode, attribute,
                                    templateNode.getAttributeBinding(attribute)
                                            .get()
                                            .getValue(overrideNode, null)));
        }
        return stateNode.get();
    }

    /**
     * Creates a new state node with all features needed for the root node of a
     * template.
     *
     * @return a new state node, not <code>null</code>
     */
    public static StateNode createRootNode() {
        return new StateNode(rootNodeFeatures);
    }

    /**
     * Creates a new state node with all features needed for a state node use as
     * a sub model.
     *
     * @param modelFeature
     *            the feature type to use for storing the model data, not
     *            <code>null</code>
     *
     * @return a new state node, not <code>null</code>
     */
    public static StateNode createSubModelNode(
            Class<? extends NodeFeature> modelFeature) {
        assert modelFeature == ModelMap.class
                || modelFeature == ModelList.class;

        return new StateNode(Collections.singletonList(modelFeature),
                TemplateOverridesMap.class);
    }

    @Override
    public Registration addPropertyChangeListener(StateNode node, String name,
            PropertyChangeListener listener) {
        throw new UnsupportedOperationException();
    }

    /**
     * Creates a new state node with all features needed for a state node use as
     * an override node.
     *
     * @return a new state node, not <code>null</code>
     */
    public static StateNode createOverrideNode() {
        return new StateNode(overrideNodeFeatures);
    }

    private void modifyChildren(StateNode node,
            BiConsumer<BasicElementStateProvider, StateNode> modifier) {
        if (templateNode.getChildCount() != 0) {
            throw new IllegalStateException(
                    "Can't add or remove children when there are children defined by the template.");
        }
        modifyOverrideNode(node, modifier);
    }

    private void modifyOverrideNode(StateNode node,
            BiConsumer<BasicElementStateProvider, StateNode> modifier) {
        StateNode overrideNode = getOrCreateOverrideNode(node);

        modifier.accept(BasicElementStateProvider.get(), overrideNode);
    }

    private void checkModifiableProperty(String name) {
        if (templateNode.getPropertyBinding(name).isPresent()) {
            throw new IllegalArgumentException(String.format(
                    "Can't modify property '%s' with binding defined in a template",
                    name));
        }
    }

    private void checkModifiableAttribute(String name) {
        Optional<BindingValueProvider> provider = templateNode
                .getAttributeBinding(name);
        if (provider.isPresent()
                && !(provider.get() instanceof StaticBindingValueProvider)) {
            throw new IllegalArgumentException(String.format(
                    "Can't modify attribute '%s' with binding defined in a template",
                    name));
        }
    }

    private boolean isStaticBindingAttribute(String attribute) {
        Optional<BindingValueProvider> binding = templateNode
                .getAttributeBinding(attribute);
        if (binding.isPresent()) {
            return binding.get() instanceof StaticBindingValueProvider;
        }
        return false;
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
}
