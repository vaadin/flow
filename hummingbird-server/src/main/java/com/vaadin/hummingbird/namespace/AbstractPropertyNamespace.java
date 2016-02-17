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
package com.vaadin.hummingbird.namespace;

import java.io.Serializable;
import java.util.Set;

import com.vaadin.hummingbird.StateNode;
import com.vaadin.hummingbird.dom.Element;

/**
 * Abstract namespace to be used as a parent for namespaces which supports
 * setting properties in a map.
 *
 * @author Vaadin
 * @since
 */
public abstract class AbstractPropertyNamespace extends MapNamespace {

    /**
     * Creates a new element property namespace for the given node.
     *
     * @param node
     *            the node that the namespace belongs to
     */
    public AbstractPropertyNamespace(StateNode node) {
        super(node);
    }

    /**
     * Sets a property to the given value.
     *
     * @param name
     *            the property name
     * @param value
     *            the value, must be a string, a boolean, a double or
     *            <code>null</code>
     */
    public void setProperty(String name, Serializable value) {
        assert isValidPropertyValue(value);
        assert isValidPropertyName(name);

        put(name, value);
    }

    /**
     * Checks if the given property value is valid.
     * <p>
     * Only to be used in assertions.
     *
     * @param name
     *            the value to validate
     * @return true if the value is valid, false otherwise
     */
    private boolean isValidPropertyValue(Serializable value) {
        if (value == null) {
            return true;
        }

        if (value instanceof String) {
            return !((String) value).endsWith(";");
        } else if (value instanceof Boolean) {
            return true;
        } else if (value instanceof Double) {
            return true;
        }

        return false;
    }

    /**
     * Checks if the given property name is valid.
     * <p>
     * Only to be used in assertions.
     *
     * @param name
     *            the name to validate
     * @return true if the name is valid, false otherwise
     */
    private boolean isValidPropertyName(String name) {
        try {
            Element.validateStylePropertyName(name);
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    /**
     * Checks whether there is a property of the given name.
     *
     * @param name
     *            the name of the property
     * @return <code>true</code> if there is a propety with the given name;
     *         <code>false</code> if there is no property
     */
    public boolean hasProperty(String name) {
        assert isValidPropertyName(name);
        return contains(name);
    }

    /**
     * Removes the given property.
     *
     * @param name
     *            the name of the property to remove
     */
    public void removeProperty(String name) {
        assert isValidPropertyName(name);
        super.remove(name);
    }

    /**
     * Removes all properties.
     *
     */
    public void removeAllProperties() {
        super.clear();
    }

    /**
     * Gets the value of the given property.
     *
     * @param name
     *            the name of the property
     * @return the property value; <code>null</code> if there is no property or
     *         if the value is explicitly set to null
     */
    public Object getProperty(String name) {
        assert isValidPropertyName(name);
        return get(name);
    }

    /**
     * Gets the property names.
     *
     * @return a set containing all the property names that have been set
     */
    public Set<String> getPropertyNames() {
        return keySet();
    }
}
