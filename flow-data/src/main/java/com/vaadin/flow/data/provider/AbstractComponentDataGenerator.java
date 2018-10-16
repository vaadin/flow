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
package com.vaadin.flow.data.provider;

import java.util.HashMap;
import java.util.Map;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.dom.Element;

/**
 * Abstract class used as base for DataGenerators that need to manage the
 * lifecycle of components, according to what items are requested or destroyed.
 * 
 * 
 * @author Vaadin Ltd
 * @since 1.0.
 *
 * @param <T>
 *            the data type
 */
public abstract class AbstractComponentDataGenerator<T>
        implements DataGenerator<T> {

    private final Map<String, Component> renderedComponents = new HashMap<>();

    @Override
    public void refreshData(T item) {
        String itemKey = getItemKey(item);
        Component oldComponent = getRenderedComponent(itemKey);
        if (oldComponent != null) {
            Component recreatedComponent = updateComponent(oldComponent, item);

            int oldId = oldComponent.getElement().getNode().getId();
            int newId = recreatedComponent.getElement().getNode().getId();
            if (oldId != newId && !oldComponent.equals(recreatedComponent)) {
                getContainer().removeChild(oldComponent.getElement());
                registerRenderedComponent(itemKey, recreatedComponent);
            }
        }
    }

    @Override
    public void destroyData(T item) {
        String itemKey = getItemKey(item);
        Component renderedComponent = renderedComponents.remove(itemKey);
        if (renderedComponent != null) {
            renderedComponent.getElement().removeFromParent();
        }
    }

    @Override
    public void destroyAllData() {
        renderedComponents.values().forEach(
                component -> component.getElement().removeFromParent());
        renderedComponents.clear();
    }

    /**
     * Gets the element where the generated components will be attached to.
     * 
     * @return the container
     */
    protected abstract Element getContainer();

    /**
     * Creates a new component based on the provided item.
     * 
     * @param item
     *            the data item, possibly <code>null</code>
     * @return a {@link Component} which represents the provided item
     */
    protected abstract Component createComponent(T item);

    /**
     * Updates an existing component after the item has been updated. By
     * default, it creates a new component instance via
     * {@link #createComponent(Object)}.
     * 
     * @param currentComponent
     *            the current component used to represent the item, not
     *            <code>null</code>
     * @param item
     *            the updated item
     * @return the component that should represent the updated item, not
     *         <code>null</code>
     */
    protected Component updateComponent(Component currentComponent, T item) {
        return createComponent(item);
    }

    /**
     * Gets a unique key for a given item. Items with the same keys are
     * considered equal.
     * 
     * @param item
     *            the model item
     * @return a unique key for the item
     */
    protected abstract String getItemKey(T item);

    /**
     * Appends the component to the container and registers it for future use
     * during the lifecycle of the generator.
     * 
     * @param itemKey
     *            the key of the model item
     * @param component
     *            the component to be attached to the container
     */
    protected void registerRenderedComponent(String itemKey,
            Component component) {

        Element element = component.getElement();
        getContainer().appendChild(element);
        renderedComponents.put(itemKey, component);
    }

    protected Component getRenderedComponent(String itemKey) {
        return renderedComponents.get(itemKey);
    }

}
