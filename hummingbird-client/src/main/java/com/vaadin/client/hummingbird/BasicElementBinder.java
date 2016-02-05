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
package com.vaadin.client.hummingbird;

import com.vaadin.client.WidgetUtil;
import com.vaadin.client.hummingbird.collection.JsCollections;
import com.vaadin.client.hummingbird.collection.JsMap;
import com.vaadin.client.hummingbird.namespace.MapNamespace;
import com.vaadin.client.hummingbird.namespace.MapProperty;
import com.vaadin.client.hummingbird.reactive.Computation;
import com.vaadin.hummingbird.shared.Namespaces;

import elemental.dom.Element;
import elemental.events.EventRemover;

/**
 * Binds element related state node namespaces to an element instance.
 *
 * @since
 * @author Vaadin Ltd
 */
public class BasicElementBinder {
    @FunctionalInterface
    private interface PropertyUser {
        public void use(MapProperty property);
    }

    private final JsMap<String, Computation> propertyBindings = JsCollections
            .map();
    private final JsMap<String, Computation> attributeBindings = JsCollections
            .map();
    private final EventRemover unregisterListener;

    private final Element element;
    private final StateNode node;

    private BasicElementBinder(StateNode node, Element element) {
        this.node = node;
        this.element = element;

        MapNamespace elementData = node
                .getMapNamespace(Namespaces.ELEMENT_DATA);

        Object nsTag = elementData.getProperty(Namespaces.TAG).getValue();
        assert nsTag == null
                || element.getTagName().toLowerCase().equals(nsTag);

        bindMap(Namespaces.ELEMENT_PROPERTIES, propertyBindings,
                this::updateProperty);

        bindMap(Namespaces.ELEMENT_ATTRIBUTES, attributeBindings,
                this::updateAttribute);

        unregisterListener = node.addUnregisterListener(e -> remove());
    }

    private void bindMap(int namespaceId, JsMap<String, Computation> bindings,
            PropertyUser user) {
        MapNamespace namespace = node.getMapNamespace(namespaceId);
        namespace.forEachProperty(
                (property, name) -> bindProperty(bindings, user, property));

        namespace.addPropertyAddListener(
                e -> bindProperty(bindings, user, e.getProperty()));
    }

    private static void bindProperty(JsMap<String, Computation> bindings,
            PropertyUser user, MapProperty property) {
        String name = property.getName();

        assert !bindings.has(name) : "There's already a binding for " + name;

        Computation computation = new Computation() {
            @Override
            protected void doRecompute() {
                user.use(property);
            }
        };

        bindings.set(name, computation);
    }

    private void updateProperty(MapProperty mapProperty) {
        String name = mapProperty.getName();

        if (mapProperty.hasValue()) {
            WidgetUtil.setJsProperty(element, name, mapProperty.getValue());
        } else if (WidgetUtil.hasOwnJsProperty(element, name)) {
            WidgetUtil.deleteJsProperty(element, name);
        } else {
            // Can't delete inherited property, so instead just clear
            // the value
            WidgetUtil.setJsProperty(element, name, null);
        }
    }

    private void updateAttribute(MapProperty mapProperty) {
        String name = mapProperty.getName();

        if (mapProperty.hasValue()) {
            element.setAttribute(name, String.valueOf(mapProperty.getValue()));
        } else {
            element.removeAttribute(name);
        }
    }

    /**
     * Removes all bindings.
     */
    public final void remove() {
        propertyBindings.forEach((computation, name) -> computation.stop());
        attributeBindings.forEach((computation, name) -> computation.stop());
        unregisterListener.remove();
    }

    /**
     * Binds a state node to an element.
     *
     * @param node
     *            the state node to bind
     * @param element
     *            the element to bind to
     *
     * @return a basic element binder
     */
    public static BasicElementBinder bind(StateNode node, Element element) {
        return new BasicElementBinder(node, element);
    }
}
