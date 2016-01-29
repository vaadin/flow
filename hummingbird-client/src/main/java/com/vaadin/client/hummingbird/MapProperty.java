/*
 * Copyright 2000-2014 Vaadin Ltd.
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

/**
 * A property in a map namespace.
 *
 * @since
 * @author Vaadin Ltd
 */
public class MapProperty {
    private final String name;
    private final MapNamespace namespace;

    private Object value;

    /**
     * Creates a new property.
     *
     * @param name
     *            the name of the property
     * @param namespace
     *            the namespace that the property belongs to
     */
    public MapProperty(String name, MapNamespace namespace) {
        this.name = name;
        this.namespace = namespace;
    }

    /**
     * Gets the name of this property.
     *
     * @return the property name
     */
    public String getName() {
        return name;
    }

    /**
     * Gets the namespace that this property belongs to.
     *
     * @return the namespace
     */
    public MapNamespace getNamespace() {
        return namespace;
    }

    /**
     * Gets the current property value.
     *
     * @return the property value
     */
    public Object getValue() {
        return value;
    }

    /**
     * Sets the property value.
     *
     * @param value
     *            the new property value
     */
    public void setValue(Object value) {
        this.value = value;
    }
}
