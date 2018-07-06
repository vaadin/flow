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
package com.vaadin.flow.server;

import java.io.Serializable;
import java.util.HashMap;

/**
 * The {@link Attributes} class represents a set of attributes.
 *
 * @author Vaadin Ltd
 * @since 1.0
 *
 */
public class Attributes implements Serializable {

    private final HashMap<String, Object> attributes = new HashMap<>();

    /**
     * Stores a value in this set.
     *
     * @see #getAttribute(String)
     *
     * @param name
     *            the name to associate the value with, can not be
     *            <code>null</code>
     * @param value
     *            the value to associate with the name, or <code>null</code> to
     *            remove a previous association.
     */
    public void setAttribute(String name, Object value) {
        if (name == null) {
            throw new IllegalArgumentException("name can not be null");
        }
        if (value != null) {
            attributes.put(name, value);
        } else {
            attributes.remove(name);
        }
    }

    /**
     * Stores a value in this set. Setting the value to <code>null</code> clears
     * the stored value.
     * <p>
     * The fully qualified name of the type is used as the name when storing the
     * value. The outcome of calling this method is thus the same as if calling
     * <p>
     * <code>setAttribute(type.getName(), value);</code>
     *
     * @see #getAttribute(Class)
     * @see #setAttribute(String, Object)
     *
     * @param <T>
     *            the attribute type
     * @param type
     *            the type that the stored value represents, can not be null
     * @param value
     *            the value to associate with the type, or <code>null</code> to
     *            remove a previous association.
     */
    public <T> void setAttribute(Class<T> type, T value) {
        if (type == null) {
            throw new IllegalArgumentException("type can not be null");
        }
        if (value != null && !type.isInstance(value)) {
            throw new IllegalArgumentException("value of type " + type.getName()
                    + " expected but got " + value.getClass().getName());
        }
        setAttribute(type.getName(), value);
    }

    /**
     * Gets a stored attribute value. If no value is stored for the name,
     * <code>null</code> is returned.
     *
     * @see #setAttribute(String, Object)
     *
     * @param name
     *            the name of the value to get, can not be <code>null</code>.
     * @return the value, or <code>null</code> if no value has been stored or if
     *         it has been set to null.
     */
    public Object getAttribute(String name) {
        if (name == null) {
            throw new IllegalArgumentException("name can not be null");
        }
        return attributes.get(name);
    }

    /**
     * Gets a stored attribute value. If no value is stored for the name,
     * <code>null</code> is returned.
     * <p>
     * The fully qualified name of the type is used as the name when getting the
     * value. The outcome of calling this method is thus the same as if calling
     * <br>
     * <br>
     * <code>getAttribute(type.getName());</code>
     *
     * @see #setAttribute(Class, Object)
     * @see #getAttribute(String)
     *
     * @param <T>
     *            the attribute type
     * @param type
     *            the type of the value to get, can not be <code>null</code>.
     * @return the value, or <code>null</code> if no value has been stored or if
     *         it has been set to null.
     */
    public <T> T getAttribute(Class<T> type) {
        if (type == null) {
            throw new IllegalArgumentException("type can not be null");
        }
        Object value = getAttribute(type.getName());
        if (value == null) {
            return null;
        } else {
            return type.cast(value);
        }
    }

    /**
     * Returns <code>true</code> if there are no attributes.
     *
     * @return <code>true</code> if there are no attributes
     */
    public boolean isEmpty() {
        return attributes.isEmpty();
    }

}
