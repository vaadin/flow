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
package com.vaadin.server;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * The {@link SessionAttributes} class represents set of Vaadin session
 * attributes.
 * 
 * @author Vaadin Ltd
 *
 */
public class SessionAttributes implements Serializable {

    private final Map<String, Object> attributes = new HashMap<>();

    private final VaadinSession session;

    /**
     * Creates an empty attributes set for given {@code session}.
     * 
     * @param session
     *            vaadin session
     */
    public SessionAttributes(VaadinSession session) {
        this.session = session;
    }

    /**
     * Stores a value in this service session. This can be used to associate
     * data with the current user so that it can be retrieved at a later point
     * from some other part of the application. Setting the value to
     * <code>null</code> clears the stored value.
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
        assert hasLock();
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
     * Stores a value in this service session. This can be used to associate
     * data with the current user so that it can be retrieved at a later point
     * from some other part of the application. Setting the value to
     * <code>null</code> clears the stored value.
     * <p>
     * The fully qualified name of the type is used as the name when storing the
     * value. The outcome of calling this method is thus the same as if calling
     * <p>
     * <code>setAttribute(type.getName(), value);</code>
     *
     * @see #getAttribute(Class)
     * @see #setAttribute(String, Object)
     *
     * @param type
     *            the type that the stored value represents, can not be null
     * @param value
     *            the value to associate with the type, or <code>null</code> to
     *            remove a previous association.
     */
    public <T> void setAttribute(Class<T> type, T value) {
        assert hasLock();
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
     * Gets a stored attribute value. If a value has been stored for the
     * session, that value is returned. If no value is stored for the name,
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
        assert hasLock();
        if (name == null) {
            throw new IllegalArgumentException("name can not be null");
        }
        return attributes.get(name);
    }

    /**
     * Gets a stored attribute value. If a value has been stored for the
     * session, that value is returned. If no value is stored for the name,
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
     * @param type
     *            the type of the value to get, can not be <code>null</code>.
     * @return the value, or <code>null</code> if no value has been stored or if
     *         it has been set to null.
     */
    public <T> T getAttribute(Class<T> type) {
        assert hasLock();
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

    private boolean hasLock() {
        return session.hasLock();
    }
}
