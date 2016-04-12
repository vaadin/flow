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
     * Sets the id of the root element of this component. The id is used with
     * various APIs to identify the element, and it should be unique on the
     * page.
     *
     * @param id
     *            the id to set, or <code>null</code> to remove any previously
     *            set id
     */
    public void setId(String id) {
        setAttribute("id", id);
    }

    /**
     * Gets the id of the root element of this component.
     *
     * @see #setId(String)
     *
     * @return the id, or <code>null</code> if no id has been set
     */
    public String getId() {
        return getAttribute("id");
    }

    /**
     * Sets the title of the root element of this component. Browsers typically
     * use the title to show a tooltip when hovering the element itself or any
     * ancestor without a title value of its own.
     *
     * @param title
     *            the title value to set, or <code>null</code> to remote any
     *            previously set title
     */
    public void setTitle(String title) {
        setAttribute("title", title);
    }

    /**
     * Sets the title of the root element of this component.
     *
     * @see #setTitle(String)
     *
     * @return the title, or <code>null</code> if no title has been set
     */
    public String getTitle() {
        return getAttribute("title");
    }

    /**
     * Adds a css class name to the root element of this component.
     *
     * @param className
     *            the css class name to add, not <code>null</code>
     */
    public void addClass(String className) {
        getClassList().add(className);
    }

    /**
     * Removes a css class name from the root element of this component.
     *
     * @param className
     *            the css class name to remove, not <code>null</code>
     * @return <code>true</code> if the class name was removed,
     *         <code>false</code> if the class list didn't contain the class
     *         name
     */
    public boolean removeClass(String className) {
        return getClassList().remove(className);
    }

    /**
     * Sets or removes the given class name for the root element of this
     * component.
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
     * Sets or removes the given attribute for the root element of this
     * component.
     *
     * @param name
     *            the name of the attribute to set or remove, not
     *            <code>null</code>
     * @param value
     *            the attribute value to set, or <code>null</code> to remove the
     *            attribute
     */
    protected void setAttribute(String name, String value) {
        if (value == null) {
            getElement().removeAttribute(name);
        } else {
            getElement().setAttribute(name, value);
        }
    }

    /**
     * Gets an attribute value from the root element of this component.
     *
     * @param name
     *            the name of the attribute, no <code>null</code>
     * @return the attribute value, or <code>null</code> if the attribute has
     *         not been set
     */
    protected String getAttribute(String name) {
        return getElement().getAttribute(name);
    }
}
