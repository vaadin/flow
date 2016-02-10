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
import java.util.Set;

import com.vaadin.hummingbird.StateNode;
import com.vaadin.hummingbird.dom.DomEventListener;
import com.vaadin.hummingbird.dom.Element;
import com.vaadin.hummingbird.dom.ElementStateProvider;
import com.vaadin.hummingbird.dom.EventRegistrationHandle;
import com.vaadin.hummingbird.namespace.ElementAttributeNamespace;
import com.vaadin.hummingbird.namespace.ElementChildrenNamespace;
import com.vaadin.hummingbird.namespace.ElementDataNamespace;
import com.vaadin.hummingbird.namespace.ElementListenersNamespace;
import com.vaadin.hummingbird.namespace.ElementPropertyNamespace;
import com.vaadin.hummingbird.namespace.Namespace;

import elemental.json.JsonValue;

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
 * @author Vaadin
 * @since
 */
public class BasicElementStateProvider implements ElementStateProvider {

    private static BasicElementStateProvider instance = new BasicElementStateProvider();

    @SuppressWarnings("unchecked")
    private static Class<? extends Namespace>[] namespaces = new Class[] {
            ElementDataNamespace.class, ElementAttributeNamespace.class,
            ElementChildrenNamespace.class, ElementPropertyNamespace.class,
            ElementListenersNamespace.class };

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
        assert Element.isValidTagName(tag) : "Invalid tag name " + tag;
        StateNode node = new StateNode(namespaces);

        node.getNamespace(ElementDataNamespace.class).setTag(tag);

        return node;
    }

    @Override
    public boolean supports(StateNode node) {
        for (Class<? extends Namespace> nsClass : namespaces) {
            if (!node.hasNamespace(nsClass)) {
                return false;
            }
        }
        if (node.getNamespace(ElementDataNamespace.class).getTag() == null) {
            return false;
        }

        return true;
    }

    @Override
    public String getTag(StateNode node) {
        return getDataNamespace(node).getTag();
    }

    /**
     * Gets the element data namespace for the given node and asserts it is
     * non-null.
     *
     * @param node
     *            the node
     * @return the data name space
     */
    private static ElementDataNamespace getDataNamespace(StateNode node) {
        return node.getNamespace(ElementDataNamespace.class);
    }

    /**
     * Gets the element attribute namespace for the given node and asserts it is
     * non-null.
     *
     * @param node
     *            the node
     * @return the data name space
     */
    private static ElementAttributeNamespace getAttributeNamespace(
            StateNode node) {
        return node.getNamespace(ElementAttributeNamespace.class);
    }

    /**
     * Gets the children data namespace for the given node and asserts it is
     * non-null.
     *
     * @param node
     *            the node
     * @return the children name space
     */
    private static ElementChildrenNamespace getChildrenNamespace(
            StateNode node) {
        return node.getNamespace(ElementChildrenNamespace.class);
    }

    private static ElementPropertyNamespace getPropertyNamespace(
            StateNode node) {
        return node.getNamespace(ElementPropertyNamespace.class);
    }

    @Override
    public void setAttribute(StateNode node, String attribute, String value) {
        assert attribute != null;
        assert attribute.equals(attribute.toLowerCase(Locale.ENGLISH));

        getAttributeNamespace(node).set(attribute, value);

    }

    @Override
    public String getAttribute(StateNode node, String attribute) {
        assert attribute != null;
        assert attribute.equals(attribute.toLowerCase(Locale.ENGLISH));

        return getAttributeNamespace(node).get(attribute);
    }

    @Override
    public boolean hasAttribute(StateNode node, String attribute) {
        assert attribute != null;
        assert attribute.equals(attribute.toLowerCase(Locale.ENGLISH));

        return getAttributeNamespace(node).has(attribute);
    }

    @Override
    public void removeAttribute(StateNode node, String attribute) {
        assert attribute != null;
        assert attribute.equals(attribute.toLowerCase(Locale.ENGLISH));

        getAttributeNamespace(node).remove(attribute);
    }

    @Override
    public Set<String> getAttributeNames(StateNode node) {
        return getAttributeNamespace(node).attributes();
    }

    @Override
    public Element getParent(StateNode node) {
        StateNode parentNode = node.getParent();
        if (parentNode == null) {
            return null;
        }

        return Element.get(parentNode);
    }

    @Override
    public int getChildCount(StateNode node) {
        return getChildrenNamespace(node).size();
    }

    @Override
    public Element getChild(StateNode node, int index) {
        assert index >= 0;
        assert index < getChildCount(node);

        return Element.get(getChildrenNamespace(node).get(index));
    }

    @Override
    public void insertChild(StateNode node, int index, Element child) {
        assert index >= 0;
        assert index <= getChildCount(node); // == if adding as last

        getChildrenNamespace(node).add(index, child.getNode());
    }

    @Override
    public void removeChild(StateNode node, int index) {
        assert index >= 0;
        assert index < getChildCount(node);

        getChildrenNamespace(node).remove(index);
    }

    @Override
    public void removeAllChildren(StateNode node) {
        getChildrenNamespace(node).clear();
    }

    @Override
    public void removeChild(StateNode node, Element child) {
        ElementChildrenNamespace childrenNamespace = getChildrenNamespace(node);
        int pos = childrenNamespace.indexOf(child.getNode());
        if (pos == -1) {
            throw new IllegalArgumentException("Not in the list");
        }
        childrenNamespace.remove(pos);

    }

    @Override
    public EventRegistrationHandle addEventListener(StateNode node,
            String eventType, DomEventListener listener) {
        ElementListenersNamespace listeners = node
                .getNamespace(ElementListenersNamespace.class);

        return listeners.add(eventType, listener);
    }

    /**
     * Gets all the namesapces used by an element node.
     *
     * @return an unmodifiable collection of namespace classes
     */
    public static Collection<Class<? extends Namespace>> getNamespaces() {
        return Collections.unmodifiableCollection(Arrays.asList(namespaces));
    }

    @Override
    public Object getProperty(StateNode node, String name) {
        assert node != null;
        assert name != null;

        return getPropertyNamespace(node).getProperty(name);
    }

    @Override
    public void setProperty(StateNode node, String name, Serializable value) {
        assert node != null;
        assert name != null;

        assert value == null || value instanceof String
                || value instanceof Boolean || value instanceof Double;

        getPropertyNamespace(node).setProperty(name, value);
    }

    @Override
    public void setJsonProperty(StateNode node, String name, JsonValue value) {
        assert node != null;
        assert name != null;
        assert value != null;

        getPropertyNamespace(node).setJsonProperty(name, value);
    }

    @Override
    public void removeValue(StateNode node, String name) {
        assert node != null;
        assert name != null;

        getPropertyNamespace(node).remove(name);
    }

    @Override
    public boolean hasProperty(StateNode node, String name) {
        assert node != null;
        assert name != null;

        return getPropertyNamespace(node).hasProperty(name);
    }

    @Override
    public Set<String> getPropertyNames(StateNode node) {
        assert node != null;

        return getPropertyNamespace(node).getPropertyNames();
    }

}
