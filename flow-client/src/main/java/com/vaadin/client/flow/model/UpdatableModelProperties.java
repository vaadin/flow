/*
 * Copyright 2000-2020 Vaadin Ltd.
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
package com.vaadin.client.flow.model;

import com.vaadin.client.flow.StateNode;
import com.vaadin.client.flow.binding.SimpleElementBindingStrategy;
import com.vaadin.client.flow.collection.JsArray;
import com.vaadin.client.flow.collection.JsCollections;
import com.vaadin.client.flow.collection.JsSet;

/**
 * The storage class for set of updatable model properties.
 * <p>
 * This class is stored inside a {@link StateNode} via
 * {@link StateNode#setNodeData(Object)} if there is any data to store at all.
 * Once it's stored in the {@link StateNode} the code which sends updates to the
 * server side when a polymer property is updated uses this data to detect
 * whether server expects the update to be sent(see
 * {@link SimpleElementBindingStrategy}).
 *
 * @author Vaadin Ltd
 * @since 1.0
 *
 */
public class UpdatableModelProperties {

    private final JsSet<String> properties = JsCollections.set();

    /**
     * Creates a new instance of storage class based on given
     * {@code properties}.
     *
     * @param properties
     *            updatable properties array
     */
    public UpdatableModelProperties(JsArray<String> properties) {
        properties.forEach(this.properties::add);
    }

    /**
     * Tests whether the {@code property} is updatable.
     *
     * @param property
     *            the property to test
     * @return {@code true} if property is updatable
     */
    public boolean isUpdatableProperty(String property) {
        return properties.has(property);
    }
}
