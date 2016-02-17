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
package com.vaadin.hummingbird.dom;

import java.io.Serializable;
import java.util.Set;

/**
 * Provides inline styles for {@link Element}s.
 *
 * @author Vaadin
 * @since
 */
public interface Style extends Serializable {
    /**
     * Gets the value of the given style property.
     *
     * @param name
     *            the style property name, not null
     * @return the style property value, or <code>null</code> if the style
     *         property has not been set
     */
    String get(String name);

    /**
     * Sets the given style property to the given value.
     *
     * @param name
     *            the style property name, not null
     * @param value
     *            the style property value
     */
    void set(String name, String value);

    /**
     * Removes the given style property if it has been set.
     *
     * @param name
     *            the style property name, not <code>null</code>
     */
    void remove(String name);

    /**
     * Removes all set style properties.
     */
    void clear();

    /**
     * Checks if the given style property has been set.
     *
     * @param name
     *            the style property name, not <code>null</code>
     *
     * @return <code>true</code> if the style property has been set,
     *         <code>false</code> otherwise
     */
    boolean has(String name);

    /**
     * Gets the defined style property names.
     *
     * @return the defined style property names
     */
    Set<String> getNames();

}
