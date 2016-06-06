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
import java.util.AbstractSet;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Predicate;
import java.util.stream.Stream;

import com.vaadin.hummingbird.StateNode;
import com.vaadin.hummingbird.dom.ClassList;
import com.vaadin.hummingbird.dom.DomEventListener;
import com.vaadin.hummingbird.dom.Element;
import com.vaadin.hummingbird.dom.ElementStateProvider;
import com.vaadin.hummingbird.dom.EventRegistrationHandle;
import com.vaadin.hummingbird.dom.Style;
import com.vaadin.hummingbird.nodefeature.ComponentMapping;
import com.vaadin.hummingbird.nodefeature.ElementAttributeMap;
import com.vaadin.hummingbird.nodefeature.ElementChildrenList;
import com.vaadin.hummingbird.nodefeature.ElementListenerMap;
import com.vaadin.hummingbird.nodefeature.ElementPropertyMap;
import com.vaadin.hummingbird.nodefeature.ModelMap;
import com.vaadin.hummingbird.nodefeature.NodeFeature;
import com.vaadin.hummingbird.nodefeature.OverrideElementData;
import com.vaadin.hummingbird.nodefeature.ParentGeneratorHolder;
import com.vaadin.hummingbird.nodefeature.SynchronizedPropertiesList;
import com.vaadin.hummingbird.nodefeature.SynchronizedPropertyEventsList;
import com.vaadin.hummingbird.nodefeature.TemplateEventHandlerNames;
import com.vaadin.hummingbird.nodefeature.TemplateMap;
import com.vaadin.hummingbird.nodefeature.TemplateOverridesMap;
import com.vaadin.hummingbird.template.BindingValueProvider;
import com.vaadin.hummingbird.template.ElementTemplateNode;
import com.vaadin.hummingbird.template.StaticBindingValueProvider;
import com.vaadin.hummingbird.template.TemplateNode;
import com.vaadin.hummingbird.util.JavaScriptSemantics;
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

    private static class BoundClassList extends AbstractSet<String>
            implements ClassList {

        private final Set<String> defaultClasses;
        private final ElementTemplateNode templateNode;
        private final StateNode node;

        public BoundClassList(ElementTemplateNode templateNode,
                StateNode node) {
            this.templateNode = templateNode;
            this.node = node;

            String[] attributeClasses = templateNode
                    .getAttributeBinding("class")
                    .map(binding -> binding.getValue(node, "").split("\\s+"))
                    .orElse(new String[0]);
            defaultClasses = new LinkedHashSet<>(
                    Arrays.asList(attributeClasses));
            // Remove defaults that are always overridden by bindings
            templateNode.getClassNames().forEach(defaultClasses::remove);
        }

        @Override
        public boolean contains(Object o) {
            if (o instanceof String) {
                return contains((String) o);
            } else {
                return false;
            }
        }

        private boolean contains(String className) {
            Optional<BindingValueProvider> binding = templateNode
                    .getClassNameBinding(className);
            if (binding.isPresent()) {
                Object bindingValue = binding.get().getValue(node);
                return JavaScriptSemantics.isTrueish(bindingValue);
            } else {
                return defaultClasses.contains(className);
            }
        }

        @Override
        public Stream<String> stream() {
            return Stream.concat(defaultClasses.stream(),
                    templateNode.getClassNames().filter(this::contains));
        }

        @Override
        public Iterator<String> iterator() {
            return stream().iterator();
        }

        @Override
        public int size() {
            return (int) stream().count();
        }
    }

    // Node features needed for a state node that represents the root of a
    // template
    @SuppressWarnings("unchecked")
    private static Class<? extends NodeFeature>[] rootNodeFeatures = new Class[] {
            ComponentMapping.class, TemplateMap.class,
            ParentGeneratorHolder.class, TemplateEventHandlerNames.class,
            TemplateOverridesMap.class, ModelMap.class };

    @SuppressWarnings("unchecked")
    private static Class<? extends NodeFeature>[] overrideNodeFeatures = Stream
            .of(OverrideElementData.class, ElementChildrenList.class,
                    ParentGeneratorHolder.class, ComponentMapping.class,
                    ElementAttributeMap.class, ElementPropertyMap.class,
                    ElementListenerMap.class, SynchronizedPropertiesList.class,
                    SynchronizedPropertyEventsList.class)
            .toArray(Class[]::new);

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
            StreamResource resource) {
        checkModifiableAttribute(attribute);
        modifyOverrideNode(node, (provider, overrideNode) -> provider
                .setAttribute(overrideNode, attribute, resource));
    }

    @Override
    public String getAttribute(StateNode node, String attribute) {
        Optional<BindingValueProvider> provider = templateNode
                .getAttributeBinding(attribute);
        String boundValue = provider.map(binding -> binding.getValue(node, ""))
                .map(Object::toString).orElse(null);
        if (provider.isPresent()
                && !(provider.get() instanceof StaticBindingValueProvider)) {
            return boundValue;
        }
        /*
         * For non-static bindings always fetch an attribute from the override
         * node first if it exists. In contrast with properties attributes may
         * be defined in the template inlined. But this definition may be
         * overriden and overriden value should be used if any.
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
         * overriden and overriden value should be used if any.
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
        Stream<String> templateAttributes = templateNode.getAttributeNames();
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

    @Override
    public EventRegistrationHandle addEventListener(StateNode node,
            String eventType, DomEventListener listener,
            String[] eventDataExpressions) {
        ElementListenerMap listeners = getOrCreateOverrideNode(node)
                .getFeature(ElementListenerMap.class);

        return listeners.add(eventType, listener, eventDataExpressions);
    }

    @Override
    public Object getProperty(StateNode node, String name) {
        if (templateNode.getPropertyBinding(name).isPresent()) {
            return templateNode.getPropertyBinding(name).get().getValue(node);
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
        // Should eventually be based on [style.foo]=bar in the template
        return new ImmutableEmptyStyle();
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
            if (!(component instanceof Template)) {
                throw new IllegalArgumentException(
                        "The component for a template root must extend "
                                + Template.class.getName());
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
     * @param dataFeature
     *            the feature type to use for storing the model data, not
     *            <code>null</code>
     *
     * @return a new state node, not <code>null</code>
     */
    public static StateNode createSubModelNode(
            Class<? extends NodeFeature> dataFeature) {
        assert dataFeature != null;
        return new StateNode(TemplateOverridesMap.class, dataFeature);
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
}
