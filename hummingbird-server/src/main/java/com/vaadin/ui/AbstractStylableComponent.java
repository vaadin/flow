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
package com.vaadin.ui;

import com.vaadin.annotations.Tag;
import com.vaadin.hummingbird.dom.ClassList;
import com.vaadin.hummingbird.dom.Element;
import com.vaadin.hummingbird.dom.Style;

/**
 * Abstract stylable component. Provides default implementation of the
 * {@link StylableComponent} interface.
 * 
 * @author Vaadin Ltd
 *
 */
public abstract class AbstractStylableComponent extends Component
        implements StylableComponent {

    /**
     * Creates a component with the element type based on the {@link Tag}
     * annotation of the sub class.
     */
    protected AbstractStylableComponent() {
        // Creates element based on @Tag
        super();
    }

    /**
     * Creates a component instance based on the given element.
     * <p>
     * For nearly all cases you want to pass an element reference but it is
     * possible to pass {@code null} to this method. If you pass {@code null}
     * you must ensure that the element is initialized using
     * {@link #setElement(Component, Element)} before {@link #getElement()} is
     * used.
     *
     * @param element
     *            the root element for the component
     */
    protected AbstractStylableComponent(Element element) {
        super(element);
    }

    /**
     * Adds a CSS class name to this component.
     *
     * @param className
     *            the CSS class name to add, not <code>null</code>
     */
    @Override
    public void addClassName(String className) {
        getClassList().add(className);
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
    @Override
    public boolean removeClassName(String className) {
        return getClassList().remove(className);
    }

    /**
     * Sets the CSS class names of this component. This method overwrites any
     * previous set class names.
     *
     * @param className
     *            a space-separated string of class names to set, or
     *            <code>null</code> to remove all class names
     */
    @Override
    public void setClassName(String className) {
        setAttribute("class", className);
    }

    /**
     * Gets the CSS class names used for this component.
     *
     * @return a space-separated string of class names, or <code>null</code> if
     *         there are no class names
     */
    @Override
    public String getClassName() {
        return getAttribute("class");
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
    @Override
    public void setClassName(String className, boolean set) {
        getClassList().set(className, set);
    }

    private ClassList getClassList() {
        return getElement().getClassList();
    }

    /**
     * Gets the style instance for managing inline styles for the element of
     * this component.
     *
     * @return the style object for the element, not <code>null</code>
     */
    @Override
    public Style getStyle() {
        return getElement().getStyle();
    }

    /**
     * Sets or removes the given attribute for this component.
     *
     * @param name
     *            the name of the attribute to set or remove, not
     *            <code>null</code>
     * @param value
     *            the attribute value to set, or <code>null</code> to remove the
     *            attribute
     */
    protected void setAttribute(String name, String value) {
        assert name != null;
        if (value == null) {
            getElement().removeAttribute(name);
        } else {
            getElement().setAttribute(name, value);
        }
    }

    /**
     * Gets an attribute value from this component.
     *
     * @param name
     *            the name of the attribute, not <code>null</code>
     * @return the attribute value, or <code>null</code> if the attribute has
     *         not been set
     */
    protected String getAttribute(String name) {
        assert name != null;
        return getElement().getAttribute(name);
    }

}
