/*
 * Copyright 2000-2019 Vaadin Ltd.
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
import java.util.function.Supplier;

/**
 * Context in which {@link VaadinService} is running.
 *
 * It is used to store service-scoped attributes.
 *
 * @author miki
 * @since 14.0.0
 */
public interface VaadinContext extends Serializable {

    /**
     * Returns value of the specified attribute, creating a default value if not
     * present.
     *
     * @param type
     *            Type of the attribute.
     * @param defaultValueSupplier
     *            {@link Supplier} of the default value, called when there is no
     *            value already present. May be {@code null}.
     * @return Value of the specified attribute.
     */
    <T> T getAttribute(Class<T> type, Supplier<T> defaultValueSupplier);

    /**
     * Returns value of the specified attribute.
     *
     * @param type
     *            Type of the attribute.
     * @return Value of the specified attribute.
     */
    default <T> T getAttribute(Class<T> type) {
        return getAttribute(type, null);
    }

    /**
     * Sets the attribute value, overriding previously existing one. Values are
     * based on exact type, meaning only one attribute of given type is possible
     * at any given time.
     *
     * @param value
     *            Value of the attribute. May not be {@code null}.
     * @see #removeAttribute(Class) for removing attributes.
     */
    <T> void setAttribute(T value);

    /**
     * Removes an attribute identified by the given type. If the attribute does
     * not exist, no action will be taken.
     * 
     * @param clazz
     *            Attribute type.
     *
     * @see #setAttribute(Object) for setting attributes.
     */
    void removeAttribute(Class<?> clazz);
}
