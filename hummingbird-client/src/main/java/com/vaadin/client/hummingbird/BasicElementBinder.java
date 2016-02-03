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
    private JsMap<String, Computation> propertyBindings = JsCollections.map();
    private EventRemover unregisterListener;

    private BasicElementBinder(StateNode node, Element element) {
        MapNamespace elementData = node
                .getMapNamespace(Namespaces.ELEMENT_DATA);

        Object nsTag = elementData.getProperty(Namespaces.TAG).getValue();
        assert nsTag == null
                || element.getTagName().toLowerCase().equals(nsTag);

        MapNamespace properties = node
                .getMapNamespace(Namespaces.ELEMENT_PROPERTIES);
        bindProperties(element, properties);

        unregisterListener = node.addUnregisterListener(e -> remove());
    }

    private void bindProperties(Element element, MapNamespace properties) {
        // Bind existing properties
        properties.forEachProperty(
                (property, name) -> bindProperty(element, property));

        // Bind any new properties as they show up
        properties.addPropertyAddListener(event -> {
            MapProperty property = event.getProperty();
            bindProperty(element, property);
        });
    }

    private void bindProperty(Element element, MapProperty property) {
        String name = property.getName();

        assert !propertyBindings.has(name) : "There's already a binding for "
                + name;

        Computation binding = new Computation() {
            @Override
            protected void doRecompute() {
                if (property.hasValue()) {
                    WidgetUtil.setJsProperty(element, property.getName(),
                            property.getValue());
                } else {
                    WidgetUtil.deleteJsProperty(element, property.getName());
                }
            }
        };

        propertyBindings.set(name, binding);
    }

    /**
     * Removes all bindings.
     */
    public final void remove() {
        propertyBindings.forEach((computation, name) -> computation.stop());
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
