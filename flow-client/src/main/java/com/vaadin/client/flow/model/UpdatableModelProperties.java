/*
 * Copyright 2000-2026 Vaadin Ltd.
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

import java.util.HashSet;
import java.util.Set;

import com.google.gwt.core.client.GWT;

import com.vaadin.client.flow.StateNode;
import com.vaadin.client.flow.binding.SimpleElementBindingStrategy;
import com.vaadin.client.flow.collection.JsArray;

/**
 * Storage class for the set of updatable model properties on a
 * {@link StateNode}, consulted by {@link SimpleElementBindingStrategy} when
 * deciding whether to push a polymer property update to the server.
 *
 * <p>
 * Under GWT this is a thin facade over the TypeScript implementation at
 * {@code src/main/frontend/internal/client/flow/model/UpdatableModelProperties.ts}.
 * The JVM path keeps a {@link HashSet}-backed copy so JUnit code that
 * instantiates this class keeps working.
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
public class UpdatableModelProperties {

    private final NativeUpdatableModelProperties delegate;
    private final Set<String> jvmProperties;

    /**
     * Creates a new instance.
     *
     * @param properties
     *            updatable property names
     */
    public UpdatableModelProperties(JsArray<String> properties) {
        if (GWT.isScript()) {
            delegate = new NativeUpdatableModelProperties(properties);
            jvmProperties = null;
        } else {
            delegate = null;
            jvmProperties = new HashSet<>();
            properties.forEach(jvmProperties::add);
        }
    }

    /**
     * Tests whether the {@code property} is updatable.
     */
    public boolean isUpdatableProperty(String property) {
        return delegate != null ? delegate.isUpdatableProperty(property)
                : jvmProperties.contains(property);
    }
}
