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
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import com.vaadin.flow.dom.ClassList;
import com.vaadin.flow.dom.DomEventListener;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.dom.ElementUtil;
import com.vaadin.flow.dom.Node;
import com.vaadin.flow.dom.NodeVisitor;
import com.vaadin.flow.dom.PropertyChangeListener;
import com.vaadin.flow.dom.Style;
import com.vaadin.flow.internal.StateNode;
import com.vaadin.flow.internal.nodefeature.AttachExistingElementFeature;
import com.vaadin.flow.internal.nodefeature.ClientDelegateHandlers;
import com.vaadin.flow.internal.nodefeature.ComponentMapping;
import com.vaadin.flow.internal.nodefeature.ElementAttributeMap;
import com.vaadin.flow.internal.nodefeature.ElementChildrenList;
import com.vaadin.flow.internal.nodefeature.ElementClassList;
import com.vaadin.flow.internal.nodefeature.ElementData;
import com.vaadin.flow.internal.nodefeature.ElementListenerMap;
import com.vaadin.flow.internal.nodefeature.ElementPropertyMap;
import com.vaadin.flow.internal.nodefeature.ElementStylePropertyMap;
import com.vaadin.flow.internal.nodefeature.NodeFeature;
import com.vaadin.flow.internal.nodefeature.NodeProperties;
import com.vaadin.flow.internal.nodefeature.ParentGeneratorHolder;
import com.vaadin.flow.internal.nodefeature.PolymerEventListenerMap;
import com.vaadin.flow.internal.nodefeature.PolymerServerEventHandlers;
import com.vaadin.flow.internal.nodefeature.ShadowRootData;
import com.vaadin.flow.internal.nodefeature.SynchronizedPropertiesList;
import com.vaadin.flow.internal.nodefeature.SynchronizedPropertyEventsList;
import com.vaadin.flow.internal.nodefeature.VirtualChildrenList;
import com.vaadin.flow.internal.nodefeature.VisibilityData;
import com.vaadin.flow.server.AbstractStreamResource;
import com.vaadin.flow.shared.Registration;
import com.vaadin.flow.template.angular.AbstractControlTemplateNode;

import elemental.json.JsonObject;
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
 * @author Vaadin Ltd
 */
public class BasicElementStateProvider extends AbstractNodeStateProvider {

    private static BasicElementStateProvider instance = new BasicElementStateProvider();

    @SuppressWarnings("unchecked")
    private static Class<? extends NodeFeature>[] features = new Class[] {
            ElementData.class, ElementAttributeMap.class,
            ElementChildrenList.class, ElementPropertyMap.class,
            ElementListenerMap.class, ElementClassList.class,
            ElementStylePropertyMap.class, SynchronizedPropertiesList.class,
            SynchronizedPropertyEventsList.class, ComponentMapping.class,
            ParentGeneratorHolder.class, PolymerServerEventHandlers.class,
            ClientDelegateHandlers.class, PolymerEventListenerMap.class,
            ShadowRootData.class, AttachExistingElementFeature.class,
            VirtualChildrenList.class, VisibilityData.class };

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
        StateNode node = new StateNode(
                Collections.singletonList(ElementData.class), features);

        node.getFeature(ElementData.class).setTag(tag);

        return node;
    }

    @Override
    public boolean supports(StateNode node) {
        if (!super.supports(node)) {
            return false;
        }
        return node.getFeature(ElementData.class).getTag() != null;
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
    public Node getParent(StateNode node) {
        StateNode parentNode = node.getParent();
        if (parentNode == null) {
            return null;
        }

        // Parent finding for all different state providers eventually delegate
        // here, so we can do shared magic here
        Optional<AbstractControlTemplateNode> parentGenerator = node
                .getFeature(ParentGeneratorHolder.class).getParentGenerator();
        if (parentGenerator.isPresent()) {
            return parentGenerator.get().getParentElement(node);
        }

        return super.getParent(node);
    }

    @Override
    public Registration addEventListener(StateNode node, String eventType,
            DomEventListener listener, String[] eventDataExpressions) {
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
        return Collections.unmodifiableCollection(Arrays
                .asList(BasicElementStateProvider.get().getProviderFeatures()));
    }

    @Override
    public Serializable getProperty(StateNode node, String name) {
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
    public void setAttribute(StateNode node, String attribute,
            AbstractStreamResource receiver) {
        assert node != null;
        assert attribute != null;
        assert receiver != null;
        getAttributeFeature(node).setResource(attribute, receiver);
    }

    @Override
    public Registration addPropertyChangeListener(StateNode node, String name,
            PropertyChangeListener listener) {
        return getPropertyFeature(node).addPropertyChangeListener(name,
                listener);
    }

    @Override
    public StateNode getShadowRoot(StateNode node) {
        return node.getFeature(ShadowRootData.class).getShadowRoot();
    }

    @Override
    public StateNode attachShadow(StateNode node) {
        assert getShadowRoot(node) == null;
        return ShadowRootStateProvider.get().createShadowRootNode(node);
    }

    @Override
    public void visit(StateNode node, NodeVisitor visitor,
            boolean visitDescendants) {
        Element element = Element.get(node);
        ElementData data = node.getFeature(ElementData.class);
        JsonValue payload = data.getPayload();

        if (payload instanceof JsonObject) {
            JsonObject object = (JsonObject) payload;
            String type = object.getString(NodeProperties.TYPE);
            if (NodeProperties.IN_MEMORY_CHILD.equals(type)) {
                visitor.visit(NodeVisitor.ElementType.VIRTUAL, element);
            } else if (NodeProperties.INJECT_BY_ID.equals(type)
                    || NodeProperties.TEMPLATE_IN_TEMPLATE.equals(type)) {
                visitor.visit(NodeVisitor.ElementType.VIRTUAL_ATTACHED,
                        element);
            } else {
                assert false : "Unexpected payload type : " + type;
            }
        } else if (payload == null) {
            visitor.visit(NodeVisitor.ElementType.REGULAR, element);
        } else {
            assert false : "Unexpected payload in element data : "
                    + payload.toJson();
        }

        if (visitDescendants) {
            visitDescendants(element, visitor);

            element.getShadowRoot()
                    .ifPresent(root -> root.accept(visitor, visitDescendants));
        }
    }

    @Override
    public void setVisible(StateNode node, boolean visible) {
        assert node.hasFeature(VisibilityData.class);
        node.getFeature(VisibilityData.class).setVisible(visible);
    }

    @Override
    public boolean isVisible(StateNode node) {
        assert node.hasFeature(VisibilityData.class);
        return node.getFeature(VisibilityData.class).isVisible();
    }

    @Override
    protected Node<?> getNode(StateNode node) {
        assert supports(node);
        return Element.get(node);
    }

    @Override
    protected Class<? extends NodeFeature>[] getProviderFeatures() {
        return features;
    }

}
