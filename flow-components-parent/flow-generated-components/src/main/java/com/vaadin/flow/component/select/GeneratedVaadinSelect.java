/*
 * Copyright 2000-2020 Vaadin Ltd.
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
package com.vaadin.flow.component.select;

import javax.annotation.Generated;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.dependency.HtmlImport;
import com.vaadin.flow.component.HasStyle;
import com.vaadin.flow.component.Focusable;
import com.vaadin.flow.component.Synchronize;
import com.vaadin.flow.component.NotSupported;
import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.shared.Registration;
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
 * {@code <vaadin-select>} is a Web Component for selecting values from a list
 * of items. The content of the the select can be populated in two ways:
 * imperatively by using renderer callback function and declaratively by using
 * Polymer's Templates.
 * </p>
 * <h3>Rendering</h3>
 * <p>
 * By default, the select uses the content provided by using the renderer
 * callback function.
 * </p>
 * <p>
 * The renderer function provides {@code root}, {@code select} arguments.
 * Generate DOM content, append it to the {@code root} element and control the
 * state of the host element by accessing {@code select}.
 * </p>
 * <p>
 * &lt;vaadin-select id=&quot;select&quot;&gt;&lt;/vaadin-select&gt;
 * {@code const select = document.querySelector('#select');select.renderer =
 * function(root, select) const listBox =
 * document.createElement('vaadin-list-box'); // append 3 <vaadin-item> elements
 * ['Jose', 'Manolo', 'Pedro'].forEach(function(name) { const item =
 * document.createElement('vaadin-item'); item.textContent = name;
 * listBox.appendChild(item); });
 * </p>
 * <p>
 * // update the content root.appendChild(listBox); };}
 * </p>
 * <p>
 * Renderer is called on initialization of new select and on its opening. DOM
 * generated during the renderer call can be reused in the next renderer call
 * and will be provided with the {@code root} argument. On first call it will be
 * empty.
 * </p>
 * <h3>Polymer Templates</h3>
 * <p>
 * Alternatively, the content can be provided with Polymer's Template. Select
 * finds the first child template and uses that in case renderer callback
 * function is not provided. You can also set a custom template using the
 * {@code template} property.
 * </p>
 * <p>
 * {@code
<vaadin-select>
<template>
<vaadin-list-box>
<vaadin-item label="foo">Foo</vaadin-item>
<vaadin-item>Bar</vaadin-item>
<vaadin-item>Baz</vaadin-item>
</vaadin-list-box>
</template>
</vaadin-select>}
 * </p>
 * <p>
 * Hint: By setting the {@code label} property of inner vaadin-items you will be
 * able to change the visual representation of the selected value in the input
 * part.
 * </p>
 * <h3>Styling</h3>
 * <p>
 * The following shadow DOM parts are available for styling:
 * </p>
 * <table>
 * <thead>
 * <tr>
 * <th>Part name</th>
 * <th>Description</th>
 * </tr>
 * </thead> <tbody>
 * <tr>
 * <td>{@code toggle-button}</td>
 * <td>The toggle button</td>
 * </tr>
 * </tbody>
 * </table>
 * <p>
 * The following state attributes are available for styling:
 * </p>
 * <table>
 * <thead>
 * <tr>
 * <th>Attribute</th>
 * <th>Description</th>
 * <th>Part name</th>
 * </tr>
 * </thead> <tbody>
 * <tr>
 * <td>{@code opened}</td>
 * <td>Set when the select is open</td>
 * <td>:host</td>
 * </tr>
 * <tr>
 * <td>{@code invalid}</td>
 * <td>Set when the element is invalid</td>
 * <td>:host</td>
 * </tr>
 * <tr>
 * <td>{@code focused}</td>
 * <td>Set when the element is focused</td>
 * <td>:host</td>
 * </tr>
 * <tr>
 * <td>{@code focus-ring}</td>
 * <td>Set when the element is keyboard focused</td>
 * <td>:host</td>
 * </tr>
 * <tr>
 * <td>{@code readonly}</td>
 * <td>Set when the select is read only</td>
 * <td>:host</td>
 * </tr>
 * </tbody>
 * </table>
 * <p>
 * {@code <vaadin-select>} element sets these custom CSS properties:
 * </p>
 * <table>
 * <thead>
 * <tr>
 * <th>Property name</th>
 * <th>Description</th>
 * <th>Theme for element</th>
 * </tr>
 * </thead> <tbody>
 * <tr>
 * <td>{@code --vaadin-select-text-field-width}</td>
 * <td>Width of the select text field</td>
 * <td>{@code vaadin-select-overlay}</td>
 * </tr>
 * </tbody>
 * </table>
 * <p>
 * See <a
 * href="https://github.com/vaadin/vaadin-themable-mixin/wiki">ThemableMixin –
 * how to apply styles for shadow parts</a>
 * </p>
 * <p>
 * In addition to {@code <vaadin-select>} itself, the following internal
 * components are themable:
 * </p>
 * <ul>
 * <li>{@code <vaadin-select-text-field>}</li>
 * <li>{@code <vaadin-select-overlay>}</li>
 * </ul>
 * <p>
 * Note: the {@code theme} attribute value set on {@code <vaadin-select>} is
 * propagated to the internal themable components listed above.
 * </p>
 */
@Generated({ "Generator: com.vaadin.generator.ComponentGenerator#1.3-SNAPSHOT",
        "WebComponent: Vaadin.SelectElement#2.0.2", "Flow#1.3-SNAPSHOT" })
@Tag("vaadin-select")
@HtmlImport("frontend://bower_components/vaadin-select/src/vaadin-select.html")
public abstract class GeneratedVaadinSelect<R extends GeneratedVaadinSelect<R, T>, T>
        extends AbstractSinglePropertyField<R, T>
        implements HasStyle, Focusable<R> {

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * Specify that this control should have input focus when the page loads.
     * <p>
     * This property is not synchronized automatically from the client side, so
     * the returned value may not be the same as in client side.
     * </p>
     * 
     * @return the {@code autofocus} property from the webcomponent
     */
    protected boolean isAutofocusBoolean() {
        return getElement().getProperty("autofocus", false);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * Specify that this control should have input focus when the page loads.
     * </p>
     * 
     * @param autofocus
     *            the boolean value to set
     */
    protected void setAutofocus(boolean autofocus) {
        getElement().setProperty("autofocus", autofocus);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * If true, the user cannot interact with this element.
     * <p>
     * This property is not synchronized automatically from the client side, so
     * the returned value may not be the same as in client side.
     * </p>
     * 
     * @return the {@code disabled} property from the webcomponent
     */
    protected boolean isDisabledBoolean() {
        return getElement().getProperty("disabled", false);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * If true, the user cannot interact with this element.
     * </p>
     * 
     * @param disabled
     *            the boolean value to set
     */
    protected void setDisabled(boolean disabled) {
        getElement().setProperty("disabled", disabled);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * Set when the select is open
     * <p>
     * This property is synchronized automatically from client side when a
     * 'opened-changed' event happens.
     * </p>
     * 
     * @return the {@code opened} property from the webcomponent
     */
    @Synchronize(property = "opened", value = "opened-changed")
    protected boolean isOpenedBoolean() {
        return getElement().getProperty("opened", false);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * Set when the select is open
     * </p>
     * 
     * @param opened
     *            the boolean value to set
     */
    protected void setOpened(boolean opened) {
        getElement().setProperty("opened", opened);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * The error message to display when the select value is invalid
     * <p>
     * This property is not synchronized automatically from the client side, so
     * the returned value may not be the same as in client side.
     * </p>
     * 
     * @return the {@code errorMessage} property from the webcomponent
     */
    protected String getErrorMessageString() {
        return getElement().getProperty("errorMessage");
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * The error message to display when the select value is invalid
     * </p>
     * 
     * @param errorMessage
     *            the String value to set
     */
    protected void setErrorMessage(String errorMessage) {
        getElement().setProperty("errorMessage",
                errorMessage == null ? "" : errorMessage);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * String used for the label element.
     * <p>
     * This property is not synchronized automatically from the client side, so
     * the returned value may not be the same as in client side.
     * </p>
     * 
     * @return the {@code label} property from the webcomponent
     */
    protected String getLabelString() {
        return getElement().getProperty("label");
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * String used for the label element.
     * </p>
     * 
     * @param label
     *            the String value to set
     */
    protected void setLabel(String label) {
        getElement().setProperty("label", label == null ? "" : label);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * The current required state of the select. True if required.
     * <p>
     * This property is not synchronized automatically from the client side, so
     * the returned value may not be the same as in client side.
     * </p>
     * 
     * @return the {@code required} property from the webcomponent
     */
    protected boolean isRequiredBoolean() {
        return getElement().getProperty("required", false);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * The current required state of the select. True if required.
     * </p>
     * 
     * @param required
     *            the boolean value to set
     */
    protected void setRequired(boolean required) {
        getElement().setProperty("required", required);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * Set to true if the value is invalid.
     * <p>
     * This property is synchronized automatically from client side when a
     * 'invalid-changed' event happens.
     * </p>
     * 
     * @return the {@code invalid} property from the webcomponent
     */
    @Synchronize(property = "invalid", value = "invalid-changed")
    protected boolean isInvalidBoolean() {
        return getElement().getProperty("invalid", false);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * Set to true if the value is invalid.
     * </p>
     * 
     * @param invalid
     *            the boolean value to set
     */
    protected void setInvalid(boolean invalid) {
        getElement().setProperty("invalid", invalid);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * The name of this element.
     * <p>
     * This property is not synchronized automatically from the client side, so
     * the returned value may not be the same as in client side.
     * </p>
     * 
     * @return the {@code name} property from the webcomponent
     */
    protected String getNameString() {
        return getElement().getProperty("name");
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * The name of this element.
     * </p>
     * 
     * @param name
     *            the String value to set
     */
    protected void setName(String name) {
        getElement().setProperty("name", name == null ? "" : name);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * A hint to the user of what can be entered in the control. The placeholder
     * will be displayed in the case that there is no item selected, or the
     * selected item has an empty string label, or the selected item has no
     * label and it's DOM content is empty.
     * <p>
     * This property is not synchronized automatically from the client side, so
     * the returned value may not be the same as in client side.
     * </p>
     * 
     * @return the {@code placeholder} property from the webcomponent
     */
    protected String getPlaceholderString() {
        return getElement().getProperty("placeholder");
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * A hint to the user of what can be entered in the control. The placeholder
     * will be displayed in the case that there is no item selected, or the
     * selected item has an empty string label, or the selected item has no
     * label and it's DOM content is empty.
     * </p>
     * 
     * @param placeholder
     *            the String value to set
     */
    protected void setPlaceholder(String placeholder) {
        getElement().setProperty("placeholder",
                placeholder == null ? "" : placeholder);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * When present, it specifies that the element is read-only.
     * <p>
     * This property is not synchronized automatically from the client side, so
     * the returned value may not be the same as in client side.
     * </p>
     * 
     * @return the {@code readonly} property from the webcomponent
     */
    protected boolean isReadonlyBoolean() {
        return getElement().getProperty("readonly", false);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * When present, it specifies that the element is read-only.
     * </p>
     * 
     * @param readonly
     *            the boolean value to set
     */
    protected void setReadonly(boolean readonly) {
        getElement().setProperty("readonly", readonly);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * Manually invoke existing renderer.
     * </p>
     */
    protected void render() {
        getElement().callFunction("render");
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * Returns true if {@code value} is valid, and sets the {@code invalid} flag
     * appropriately.
     * </p>
     * <p>
     * This function is not supported by Flow because it returns a
     * <code>boolean</code>. Functions with return types different than void are
     * not supported at this moment.
     */
    @NotSupported
    protected void validate() {
    }

    public static class OpenedChangeEvent<R extends GeneratedVaadinSelect<R, ?>>
            extends ComponentEvent<R> {
        private final boolean opened;

        public OpenedChangeEvent(R source, boolean fromClient) {
            super(source, fromClient);
            this.opened = source.isOpenedBoolean();
        }

        public boolean isOpened() {
            return opened;
        }
    }

    /**
     * Adds a listener for {@code opened-changed} events fired by the
     * webcomponent.
     * 
     * @param listener
     *            the listener
     * @return a {@link Registration} for removing the event listener
     */
    protected Registration addOpenedChangeListener(
            ComponentEventListener<OpenedChangeEvent<R>> listener) {
        return getElement()
                .addPropertyChangeListener("opened",
                        event -> listener.onComponentEvent(
                                new OpenedChangeEvent<R>((R) this,
                                        event.isUserOriginated())));
    }

    public static class InvalidChangeEvent<R extends GeneratedVaadinSelect<R, ?>>
            extends ComponentEvent<R> {
        private final boolean invalid;

        public InvalidChangeEvent(R source, boolean fromClient) {
            super(source, fromClient);
            this.invalid = source.isInvalidBoolean();
        }

        public boolean isInvalid() {
            return invalid;
        }
    }

    /**
     * Adds a listener for {@code invalid-changed} events fired by the
     * webcomponent.
     * 
     * @param listener
     *            the listener
     * @return a {@link Registration} for removing the event listener
     */
    protected Registration addInvalidChangeListener(
            ComponentEventListener<InvalidChangeEvent<R>> listener) {
        return getElement()
                .addPropertyChangeListener("invalid",
                        event -> listener.onComponentEvent(
                                new InvalidChangeEvent<R>((R) this,
                                        event.isUserOriginated())));
    }

    /**
     * Adds the given components as children of this component at the slot
     * 'prefix'.
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
    protected void addToPrefix(Component... components) {
        for (Component component : components) {
            component.getElement().setAttribute("slot", "prefix");
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
     * Constructs a new GeneratedVaadinSelect component with the given
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
    public <P> GeneratedVaadinSelect(T initialValue, T defaultValue,
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
     * Constructs a new GeneratedVaadinSelect component with the given
     * arguments.
     * 
     * @param initialValue
     *            the initial value to set to the value
     * @param defaultValue
     *            the default value to use if the value isn't defined
     * @param acceptNullValues
     *            whether <code>null</code> is accepted as a model value
     */
    public GeneratedVaadinSelect(T initialValue, T defaultValue,
            boolean acceptNullValues) {
        super("value", defaultValue, acceptNullValues);
        if (initialValue != null) {
            setModelValue(initialValue, false);
            setPresentationValue(initialValue);
        }
    }

    /**
     * Constructs a new GeneratedVaadinSelect component with the given
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
    public <P> GeneratedVaadinSelect(T initialValue, T defaultValue,
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
    public GeneratedVaadinSelect() {
        this(null, null, null, (SerializableFunction) null,
                (SerializableFunction) null);
    }
}