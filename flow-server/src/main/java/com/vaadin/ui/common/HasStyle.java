/*
 * Copyright 2000-2017 Vaadin Ltd.
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
package com.vaadin.ui.common;

import java.util.HashSet;
import java.util.Set;

import com.vaadin.flow.dom.Style;
import com.vaadin.ui.Component;

/**
 * Represents {@link Component} which has class attribute and inline styles.
 *
 * @author Vaadin Ltd
 *
 */
public interface HasStyle extends HasElement {

    /**
     * Adds a CSS class name to this component.
     *
     * @param className
     *            the CSS class name to add, not <code>null</code>
     */
    default void addClassName(String className) {
        getElement().getClassList().add(className);
    }

    /**
     * Removes a CSS class name from this component.
     *
     * @param className
     *            the CSS class name to remove, not <code>null</code>
     * @return <code>true</code> if the class name was removed,
     *         <code>false</code> if the class list didn't contain the class
     *         name
     */
    default boolean removeClassName(String className) {
        return getElement().getClassList().remove(className);
    }

    /**
     * Sets the CSS class names of this component. This method overwrites any
     * previous set class names.
     *
     * @param className
     *            a space-separated string of class names to set, or
     *            <code>null</code> to remove all class names
     */
    default void setClassName(String className) {
        getElement().setAttribute("class", className);
    }

    /**
     * Gets the CSS class names for this component.
     *
     * @return a space-separated string of class names, or <code>null</code> if
     *         there are no class names
     */
    default String getClassName() {
        return getElement().getAttribute("class");
    }

    /**
     * Gets the CSS class names for this component.
     * <p>
     * Note that classes cannot be added or removed through the returned
     * collection.
     * 
     * @return a set of class names, never <code>null</code>
     */
    default Set<String> getClassNames() {
         return new HashSet<>(getElement().getClassList());
    }

    /**
     * Sets or removes the given class name for this component.
     *
     * @param className
     *            the class name to set or remove, not <code>null</code>
     * @param set
     *            <code>true</code> to set the class name, <code>false</code> to
     *            remove it
     */
    default void setClassName(String className, boolean set) {
        getElement().getClassList().set(className, set);
    }

    /**
     * Checks if the component has the given class name.
     *
     * @param className
     *            the class name to check for
     * @return <code>true</code> if the component has the given class name,
     *         <code>false</code> otherwise
     */
    default boolean hasClassName(String className) {
        return getElement().getClassList().contains(className);
    }

    /**
     * Gets the style instance for managing inline styles for the element of
     * this component.
     *
     * @return the style object for the element, not <code>null</code>
     */
    default Style getStyle() {
        return getElement().getStyle();
    }
}
