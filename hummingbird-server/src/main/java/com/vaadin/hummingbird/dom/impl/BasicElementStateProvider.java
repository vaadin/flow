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
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import com.vaadin.hummingbird.StateNode;
import com.vaadin.hummingbird.dom.ClassList;
import com.vaadin.hummingbird.dom.DomEventListener;
import com.vaadin.hummingbird.dom.Element;
import com.vaadin.hummingbird.dom.ElementStateProvider;
import com.vaadin.hummingbird.dom.ElementUtil;
import com.vaadin.hummingbird.dom.EventRegistrationHandle;
import com.vaadin.hummingbird.dom.Style;
import com.vaadin.hummingbird.nodefeature.ComponentMapping;
import com.vaadin.hummingbird.nodefeature.ElementAttributeMap;
import com.vaadin.hummingbird.nodefeature.ElementChildrenList;
import com.vaadin.hummingbird.nodefeature.ElementClassList;
import com.vaadin.hummingbird.nodefeature.ElementData;
import com.vaadin.hummingbird.nodefeature.ElementListenerMap;
import com.vaadin.hummingbird.nodefeature.ElementPropertyMap;
import com.vaadin.hummingbird.nodefeature.ElementStylePropertyMap;
import com.vaadin.hummingbird.nodefeature.NodeFeature;
import com.vaadin.hummingbird.nodefeature.ParentGeneratorHolder;
import com.vaadin.hummingbird.nodefeature.SynchronizedPropertiesList;
import com.vaadin.hummingbird.nodefeature.SynchronizedPropertyEventsList;
import com.vaadin.hummingbird.template.VariableTemplateNode;
import com.vaadin.server.StreamResource;
import com.vaadin.ui.Component;

/**
 * Implementation which stores data for basic elements, i.e. elements which are
 * not bound to any template and have no special functionality.
 * <p>
 * This should be considered a low level class focusing on performance and
 * leaving most sanity checks to the caller.
 * <p>
 * The data is stored directly in the state node but this should be considered
 * an implementation detail which can change.
 *
 * @author Vaadin Ltd
 */
public class BasicElementStateProvider implements ElementStateProvider {

    private static BasicElementStateProvider instance = new BasicElementStateProvider();

    @SuppressWarnings("unchecked")
    private static Class<? extends NodeFeature>[] features = new Class[] {
            ElementData.class, ElementAttributeMap.class,
            ElementChildrenList.class, ElementPropertyMap.class,
            ElementListenerMap.class, ElementClassList.class,
            ElementStylePropertyMap.class, SynchronizedPropertiesList.class,
            SynchronizedPropertyEventsList.class, ComponentMapping.class,
            ParentGeneratorHolder.class };

    private BasicElementStateProvider() {
        // Not meant to be sub classed and only once instance should ever exist
    }

    /**
     * Gets the one and only instance.
     *
     * @return the instance to use for all basic elements
     */
    public static BasicElementStateProvider get() {
        return instance;
    }

    /**
     * Creates a compatible element state node using the given {@code tag}.
     *
     * @param tag
     *            the tag to use for the element
     * @return a initialized and compatible state node
     */
    public static StateNode createStateNode(String tag) {
        assert ElementUtil.isValidTagName(tag) : "Invalid tag name " + tag;
        StateNode node = new StateNode(features);

        node.getFeature(ElementData.class).setTag(tag);

        return node;
    }

    @Override
    public boolean supports(StateNode node) {
        for (Class<? extends NodeFeature> nsClass : features) {
            if (!node.hasFeature(nsClass)) {
                return false;
            }
        }
        if (node.getFeature(ElementData.class).getTag() == null) {
            return false;
        }

        return true;
    }

    @Override
    public String getTag(StateNode node) {
        return getDataFeature(node).getTag();
    }

    /**
     * Gets the element data feature for the given node and asserts it is
     * non-null.
     *
     * @param node
     *            the node
     * @return the data feature
     */
    private static ElementData getDataFeature(StateNode node) {
        return node.getFeature(ElementData.class);
    }

    /**
     * Gets the element attribute feature for the given node and asserts it is
     * non-null.
     *
     * @param node
     *            the node
     * @return the attribute feature
     */
    private static ElementAttributeMap getAttributeFeature(StateNode node) {
        return node.getFeature(ElementAttributeMap.class);
    }

    /**
     * Gets the children data feature for the given node and asserts it is
     * non-null.
     *
     * @param node
     *            the node
     * @return the children feature
     */
    private static ElementChildrenList getChildrenFeature(StateNode node) {
        return node.getFeature(ElementChildrenList.class);
    }

    /**
     * Gets the property feature for the given node and asserts it is non-null.
     *
     * @param node
     *            the node
     * @return the property feature
     */
    private static ElementPropertyMap getPropertyFeature(StateNode node) {
        return node.getFeature(ElementPropertyMap.class);
    }

    @Override
    public void setAttribute(StateNode node, String attribute, String value) {
        assert attribute != null;
        assert attribute.equals(attribute.toLowerCase(Locale.ENGLISH));

        getAttributeFeature(node).set(attribute, value);

    }

    @Override
    public String getAttribute(StateNode node, String attribute) {
        assert attribute != null;
        assert attribute.equals(attribute.toLowerCase(Locale.ENGLISH));

        return getAttributeFeature(node).get(attribute);
    }

    @Override
    public boolean hasAttribute(StateNode node, String attribute) {
        assert attribute != null;
        assert attribute.equals(attribute.toLowerCase(Locale.ENGLISH));

        return getAttributeFeature(node).has(attribute);
    }

    @Override
    public void removeAttribute(StateNode node, String attribute) {
        assert attribute != null;
        assert attribute.equals(attribute.toLowerCase(Locale.ENGLISH));

        getAttributeFeature(node).remove(attribute);
    }

    @Override
    public Stream<String> getAttributeNames(StateNode node) {
        return getAttributeFeature(node).attributes();
    }

    @Override
    public Element getParent(StateNode node) {
        StateNode parentNode = node.getParent();
        if (parentNode == null) {
            return null;
        }

        // Parent finding for all different state providers eventually delegate
        // here, so we can do shared magic here
        Optional<VariableTemplateNode> parentGenerator = node
                .getFeature(ParentGeneratorHolder.class).getParentGenerator();
        if (parentGenerator.isPresent()) {
            return parentGenerator.get().getParent(node);
        }

        return Element.get(parentNode);
    }

    @Override
    public int getChildCount(StateNode node) {
        return getChildrenFeature(node).size();
    }

    @Override
    public Element getChild(StateNode node, int index) {
        assert index >= 0;
        assert index < getChildCount(node);

        return Element.get(getChildrenFeature(node).get(index));
    }

    @Override
    public void insertChild(StateNode node, int index, Element child) {
        assert index >= 0;
        assert index <= getChildCount(node); // == if adding as last

        getChildrenFeature(node).add(index, child.getNode());
    }

    @Override
    public void removeChild(StateNode node, int index) {
        assert index >= 0;
        assert index < getChildCount(node);

        getChildrenFeature(node).remove(index);
    }

    @Override
    public void removeAllChildren(StateNode node) {
        getChildrenFeature(node).clear();
    }

    @Override
    public void removeChild(StateNode node, Element child) {
        ElementChildrenList childrenFeature = getChildrenFeature(node);
        int pos = childrenFeature.indexOf(child.getNode());
        if (pos == -1) {
            throw new IllegalArgumentException("Not in the list");
        }
        childrenFeature.remove(pos);

    }

    @Override
    public EventRegistrationHandle addEventListener(StateNode node,
            String eventType, DomEventListener listener,
            String[] eventDataExpressions) {
        ElementListenerMap listeners = node
                .getFeature(ElementListenerMap.class);

        return listeners.add(eventType, listener, eventDataExpressions);
    }

    /**
     * Gets all the features used by an element node.
     *
     * @return an unmodifiable collection of feature classes
     */
    public static Collection<Class<? extends NodeFeature>> getFeatures() {
        return Collections.unmodifiableCollection(Arrays.asList(features));
    }

    @Override
    public Object getProperty(StateNode node, String name) {
        assert node != null;
        assert name != null;

        return getPropertyFeature(node).getProperty(name);
    }

    @Override
    public void setProperty(StateNode node, String name, Serializable value,
            boolean emitChange) {
        assert node != null;
        assert name != null;

        getPropertyFeature(node).setProperty(name, value, emitChange);
    }

    @Override
    public void removeProperty(StateNode node, String name) {
        assert node != null;
        assert name != null;

        getPropertyFeature(node).removeProperty(name);
    }

    @Override
    public boolean hasProperty(StateNode node, String name) {
        assert node != null;
        assert name != null;

        return getPropertyFeature(node).hasProperty(name);
    }

    @Override
    public Stream<String> getPropertyNames(StateNode node) {
        assert node != null;

        return getPropertyFeature(node).getPropertyNames();
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
        return node.getFeature(ElementClassList.class).getClassList();
    }

    @Override
    public Style getStyle(StateNode node) {
        return node.getFeature(ElementStylePropertyMap.class).getStyle();
    }

    @Override
    public Set<String> getSynchronizedProperties(StateNode node) {
        return node.getFeature(SynchronizedPropertiesList.class)
                .getSynchronizedProperties();
    }

    @Override
    public Set<String> getSynchronizedPropertyEvents(StateNode node) {
        return node.getFeature(SynchronizedPropertyEventsList.class)
                .getSynchronizedPropertyEvents();
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

    @Override
    public void setAttribute(StateNode node, String attribute,
            StreamResource resource) {
        assert node != null;
        assert attribute != null;
        assert resource != null;
        getAttributeFeature(node).setResource(attribute, resource);
    }

}
