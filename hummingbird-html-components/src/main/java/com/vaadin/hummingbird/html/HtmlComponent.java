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
package com.vaadin.hummingbird.html;

import com.vaadin.annotations.Tag;
import com.vaadin.hummingbird.dom.ClassList;
import com.vaadin.hummingbird.dom.Element;
import com.vaadin.hummingbird.dom.Style;
import com.vaadin.ui.Component;

/**
 * Base class for a {@link Component} that represents a single built-in HTML
 * element.
 *
 * @since
 * @author Vaadin Ltd
 */
public class HtmlComponent extends Component {

    /**
     * Creates a component with the element type based on the {@link Tag}
     * annotation of the sub class.
     */
    protected HtmlComponent() {
        // Creates element based on @Tag
        super();
    }

    /**
     * Creates a component with a new element with the given tag name.
     *
     * @param tagName
     *            the tag name of the element to use for this component, not
     *            <code>null</code>
     */
    public HtmlComponent(String tagName) {
        super(new Element(tagName));
    }

    /**
     * Sets the title of this component. Browsers typically use the title to
     * show a tooltip when hovering an element or any descendant without a title
     * value of its own.
     *
     * @param title
     *            the title value to set, or <code>null</code> to remove any
     *            previously set title
     */
    public void setTitle(String title) {
        setAttribute("title", title);
    }

    /**
     * Gets the title of this component.
     *
     * @see #setTitle(String)
     *
     * @return the title, or <code>null</code> if no title has been set
     */
    public String getTitle() {
        return getAttribute("title");
    }

    /**
     * Adds a CSS class name to this component.
     *
     * @param className
     *            the CSS class name to add, not <code>null</code>
     */
    public void addClass(String className) {
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
    public boolean removeClass(String className) {
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
    public void setClassName(String className) {
        setAttribute("class", className);
    }

    /**
     * Gets the CSS class names used for this component.
     *
     * @return a space-separated string of class names, or <code>null</code> if
     *         there are no class names
     */
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
    public void setClass(String className, boolean set) {
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
