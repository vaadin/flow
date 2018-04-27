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
package com.vaadin.flow.component.checkbox;

import javax.annotation.Generated;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.dependency.HtmlImport;
import com.vaadin.flow.component.HasStyle;
import com.vaadin.flow.component.Focusable;
import com.vaadin.flow.component.ClickNotifier;
import com.vaadin.flow.component.Synchronize;
import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.shared.Registration;
import com.vaadin.flow.function.SerializableFunction;
import com.vaadin.flow.function.SerializableBiFunction;
import com.vaadin.flow.component.AbstractSinglePropertyField;

/**
 * <p>
 * Description copied from corresponding location in WebComponent:
 * </p>
 * <p>
 * {@code <vaadin-checkbox>} is a Polymer 2 element for customized checkboxes.
 * </p>
 * <p>
 * &lt;vaadin-checkbox&gt; Make my profile visible &lt;/vaadin-checkbox&gt;
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
 * <td>{@code checkbox}</td>
 * <td>The checkbox element</td>
 * </tr>
 * <tr>
 * <td>{@code label}</td>
 * <td>The label content element</td>
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
 * </tr>
 * </thead> <tbody>
 * <tr>
 * <td>{@code active}</td>
 * <td>Set when the checkbox is pressed down, either with mouse, touch or the
 * keyboard.</td>
 * </tr>
 * <tr>
 * <td>{@code disabled}</td>
 * <td>Set when the checkbox is disabled.</td>
 * </tr>
 * <tr>
 * <td>{@code focus-ring}</td>
 * <td>Set when the checkbox is focused using the keyboard.</td>
 * </tr>
 * <tr>
 * <td>{@code focused}</td>
 * <td>Set when the checkbox is focused.</td>
 * </tr>
 * <tr>
 * <td>{@code indeterminate}</td>
 * <td>Set when the checkbox is in indeterminate mode.</td>
 * </tr>
 * <tr>
 * <td>{@code checked}</td>
 * <td>Set when the checkbox is checked.</td>
 * </tr>
 * </tbody>
 * </table>
 * <p>
 * See <a
 * href="https://github.com/vaadin/vaadin-themable-mixin/wiki">ThemableMixin –
 * how to apply styles for shadow parts</a>
 * </p>
 */
@Generated({ "Generator: com.vaadin.generator.ComponentGenerator#1.0-SNAPSHOT",
        "WebComponent: Vaadin.CheckboxElement#2.0.0", "Flow#1.0-SNAPSHOT" })
@Tag("vaadin-checkbox")
@HtmlImport("frontend://bower_components/vaadin-checkbox/src/vaadin-checkbox.html")
public abstract class GeneratedVaadinCheckbox<R extends GeneratedVaadinCheckbox<R, T>, T>
        extends AbstractSinglePropertyField<R, T>
        implements HasStyle, Focusable<R>, ClickNotifier<R> {

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
     * This property is not synchronized automatically from the client side, so
     * the returned value may not be the same as in client side.
     * 
     * @return the {@code name} property from the webcomponent
     */
    protected String getNameString() {
        return getElement().getProperty("name");
    }

    /**
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
     * Indeterminate state of the checkbox when it's neither checked nor
     * unchecked, but undetermined.
     * https://developer.mozilla.org/en-US/docs/Web/
     * HTML/Element/input/checkbox#Indeterminate_state_checkboxes
     * <p>
     * This property is synchronized automatically from client side when a
     * 'indeterminate-changed' event happens.
     * </p>
     * 
     * @return the {@code indeterminate} property from the webcomponent
     */
    @Synchronize(property = "indeterminate", value = "indeterminate-changed")
    protected boolean isIndeterminateBoolean() {
        return getElement().getProperty("indeterminate", false);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * Indeterminate state of the checkbox when it's neither checked nor
     * unchecked, but undetermined.
     * https://developer.mozilla.org/en-US/docs/Web/
     * HTML/Element/input/checkbox#Indeterminate_state_checkboxes
     * </p>
     * 
     * @param indeterminate
     *            the boolean value to set
     */
    protected void setIndeterminate(boolean indeterminate) {
        getElement().setProperty("indeterminate", indeterminate);
    }

    public static class CheckedChangeEvent<R extends GeneratedVaadinCheckbox<R, ?>>
            extends ComponentEvent<R> {
        public CheckedChangeEvent(R source, boolean fromClient) {
            super(source, fromClient);
        }
    }

    /**
     * Adds a listener for {@code checked-changed} events fired by the
     * webcomponent.
     * 
     * @param listener
     *            the listener
     * @return a {@link Registration} for removing the event listener
     */
    protected Registration addCheckedChangeListener(
            ComponentEventListener<CheckedChangeEvent<R>> listener) {
        return getElement()
                .addPropertyChangeListener("checked",
                        event -> listener.onComponentEvent(
                                new CheckedChangeEvent<R>((R) this,
                                        event.isUserOriginated())));
    }

    public static class IndeterminateChangeEvent<R extends GeneratedVaadinCheckbox<R, ?>>
            extends ComponentEvent<R> {
        private final boolean indeterminate;

        public IndeterminateChangeEvent(R source, boolean fromClient) {
            super(source, fromClient);
            this.indeterminate = source.isIndeterminateBoolean();
        }

        public boolean isIndeterminate() {
            return indeterminate;
        }
    }

    /**
     * Adds a listener for {@code indeterminate-changed} events fired by the
     * webcomponent.
     * 
     * @param listener
     *            the listener
     * @return a {@link Registration} for removing the event listener
     */
    protected Registration addIndeterminateChangeListener(
            ComponentEventListener<IndeterminateChangeEvent<R>> listener) {
        return getElement().addPropertyChangeListener("indeterminate",
                event -> listener.onComponentEvent(
                        new IndeterminateChangeEvent<R>((R) this,
                                event.isUserOriginated())));
    }

    /**
     * Constructor.
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
    public <P> GeneratedVaadinCheckbox(T initialValue, T defaultValue,
            Class<P> elementPropertyType,
            SerializableFunction<P, T> presentationToModel,
            SerializableFunction<T, P> modelToPresentation) {
        super("checked", defaultValue, elementPropertyType, presentationToModel,
                modelToPresentation);
        if (initialValue != null) {
            setModelValue(initialValue, false);
            setPresentationValue(initialValue);
        }
    }

    /**
     * Constructor.
     * 
     * @param initialValue
     *            the initial value to set to the value
     * @param defaultValue
     *            the default value to use if the value isn't defined
     * @param acceptNullValues
     *            whether <code>null</code> is accepted as a model value
     */
    public GeneratedVaadinCheckbox(T initialValue, T defaultValue,
            boolean acceptNullValues) {
        super("checked", defaultValue, acceptNullValues);
        if (initialValue != null) {
            setModelValue(initialValue, false);
            setPresentationValue(initialValue);
        }
    }

    /**
     * Constructor.
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
    public <P> GeneratedVaadinCheckbox(T initialValue, T defaultValue,
            Class<P> elementPropertyType,
            SerializableBiFunction<R, P, T> presentationToModel,
            SerializableBiFunction<R, T, P> modelToPresentation) {
        super("checked", defaultValue, elementPropertyType, presentationToModel,
                modelToPresentation);
        if (initialValue != null) {
            setModelValue(initialValue, false);
            setPresentationValue(initialValue);
        }
    }

    /**
     * Default constructor.
     */
    public GeneratedVaadinCheckbox() {
        this(null, null, null, (SerializableFunction) null,
                (SerializableFunction) null);
    }
}