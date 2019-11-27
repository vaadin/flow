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
package com.vaadin.flow.component.grid;

import javax.annotation.Generated;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.dependency.HtmlImport;
import com.vaadin.flow.component.HasStyle;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.function.SerializableFunction;
import com.vaadin.flow.function.SerializableBiFunction;
import com.vaadin.flow.component.AbstractSinglePropertyField;

/**
 * <p>
 * Description copied from corresponding location in WebComponent:
 * </p>
 * <p>
 * {@code <vaadin-grid-filter>} is a helper element for the
 * {@code <vaadin-grid>} that provides out-of-the-box UI controls, and handlers
 * for filtering the grid data.
 * </p>
 * <h4>Example:</h4>
 * <p>
 * &lt;vaadin-grid-column&gt; &lt;template class=&quot;header&quot;&gt;
 * &lt;vaadin-grid-filter
 * path=&quot;name.first&quot;&gt;&lt;/vaadin-grid-filter&gt; &lt;/template&gt;
 * &lt;template&gt;[[item.name.first]]&lt;/template&gt;
 * &lt;/vaadin-grid-column&gt;
 * </p>
 */
@Generated({ "Generator: com.vaadin.generator.ComponentGenerator#1.2-SNAPSHOT",
        "WebComponent: Vaadin.GridFilterElement#5.2.1", "Flow#1.2-SNAPSHOT" })
@Tag("vaadin-grid-filter")
@HtmlImport("frontend://bower_components/vaadin-grid/src/vaadin-grid-filter.html")
public abstract class GeneratedVaadinGridFilter<R extends GeneratedVaadinGridFilter<R, T>, T>
        extends AbstractSinglePropertyField<R, T> implements HasStyle {

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * JS Path of the property in the item used for filtering the data.
     * <p>
     * This property is not synchronized automatically from the client side, so
     * the returned value may not be the same as in client side.
     * </p>
     * 
     * @return the {@code path} property from the webcomponent
     */
    protected String getPathString() {
        return getElement().getProperty("path");
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * JS Path of the property in the item used for filtering the data.
     * </p>
     * 
     * @param path
     *            the String value to set
     */
    protected void setPath(String path) {
        getElement().setProperty("path", path == null ? "" : path);
    }

    protected void focus() {
        getElement().callFunction("focus");
    }

    /**
     * Adds the given components as children of this component at the slot
     * 'filter'.
     * 
     * @param components
     *            The components to add.
     * @see <a
     *      href="https://developer.mozilla.org/en-US/docs/Web/HTML/Element/slot">MDN
     *      page about slots</a>
     * @see <a
     *      href="https://html.spec.whatwg.org/multipage/scripting.html#the-slot-element">Spec
     *      website about slots</a>
     */
    protected void addToFilter(Component... components) {
        for (Component component : components) {
            component.getElement().setAttribute("slot", "filter");
            getElement().appendChild(component.getElement());
        }
    }

    /**
     * Removes the given child components from this component.
     * 
     * @param components
     *            The components to remove.
     * @throws IllegalArgumentException
     *             if any of the components is not a child of this component.
     */
    protected void remove(Component... components) {
        for (Component component : components) {
            if (getElement().equals(component.getElement().getParent())) {
                component.getElement().removeAttribute("slot");
                getElement().removeChild(component.getElement());
            } else {
                throw new IllegalArgumentException("The given component ("
                        + component + ") is not a child of this component");
            }
        }
    }

    /**
     * Removes all contents from this component, this includes child components,
     * text content as well as child elements that have been added directly to
     * this component using the {@link Element} API.
     */
    protected void removeAll() {
        getElement().getChildren()
                .forEach(child -> child.removeAttribute("slot"));
        getElement().removeAllChildren();
    }

    /**
     * Constructs a new GeneratedVaadinGridFilter component with the given
     * arguments.
     * 
     * @param initialValue
     *            the initial value to set to the value
     * @param defaultValue
     *            the default value to use if the value isn't defined
     * @param elementPropertyType
     *            the type of the element property
     * @param presentationToModel
     *            a function that converts a string value to a model value
     * @param modelToPresentation
     *            a function that converts a model value to a string value
     * @param <P>
     *            the property type
     */
    public <P> GeneratedVaadinGridFilter(T initialValue, T defaultValue,
            Class<P> elementPropertyType,
            SerializableFunction<P, T> presentationToModel,
            SerializableFunction<T, P> modelToPresentation) {
        super("value", defaultValue, elementPropertyType, presentationToModel,
                modelToPresentation);
        if (initialValue != null) {
            setModelValue(initialValue, false);
            setPresentationValue(initialValue);
        }
    }

    /**
     * Constructs a new GeneratedVaadinGridFilter component with the given
     * arguments.
     * 
     * @param initialValue
     *            the initial value to set to the value
     * @param defaultValue
     *            the default value to use if the value isn't defined
     * @param acceptNullValues
     *            whether <code>null</code> is accepted as a model value
     */
    public GeneratedVaadinGridFilter(T initialValue, T defaultValue,
            boolean acceptNullValues) {
        super("value", defaultValue, acceptNullValues);
        if (initialValue != null) {
            setModelValue(initialValue, false);
            setPresentationValue(initialValue);
        }
    }

    /**
     * Constructs a new GeneratedVaadinGridFilter component with the given
     * arguments.
     * 
     * @param initialValue
     *            the initial value to set to the value
     * @param defaultValue
     *            the default value to use if the value isn't defined
     * @param elementPropertyType
     *            the type of the element property
     * @param presentationToModel
     *            a function that accepts this component and a property value
     *            and returns a model value
     * @param modelToPresentation
     *            a function that accepts this component and a model value and
     *            returns a property value
     * @param <P>
     *            the property type
     */
    public <P> GeneratedVaadinGridFilter(T initialValue, T defaultValue,
            Class<P> elementPropertyType,
            SerializableBiFunction<R, P, T> presentationToModel,
            SerializableBiFunction<R, T, P> modelToPresentation) {
        super("value", defaultValue, elementPropertyType, presentationToModel,
                modelToPresentation);
        if (initialValue != null) {
            setModelValue(initialValue, false);
            setPresentationValue(initialValue);
        }
    }

    /**
     * Default constructor.
     */
    public GeneratedVaadinGridFilter() {
        this(null, null, null, (SerializableFunction) null,
                (SerializableFunction) null);
    }
}