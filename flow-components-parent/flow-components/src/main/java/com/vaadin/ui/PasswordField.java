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

package com.vaadin.ui;

import com.vaadin.annotations.DomEvent;
import com.vaadin.annotations.Synchronize;
import com.vaadin.components.data.HasValue;
import com.vaadin.flow.event.ComponentEventListener;
import com.vaadin.generated.vaadin.text.field.GeneratedVaadinPasswordField;
import com.vaadin.shared.Registration;

import elemental.json.JsonObject;

/**
 * Server-side component for the {@code vaadin-password-field} element.
 *
 * @author Vaadin Ltd.
 */
public class PasswordField extends GeneratedVaadinPasswordField<PasswordField>
        implements HasSize, Focusable<PasswordField>,
        HasValue<PasswordField, String> {

    /**
     * Constructs an empty {@code PasswordField}.
     * <p>
     * Using this constructor, any value previously set at the client-side is
     * cleared.
     */
    public PasswordField() {
        getElement().synchronizeProperty("hasValue", "value-changed");
        getElement().synchronizeProperty("passwordVisible", "password-visible-changed");
        clear();
    }

    /**
     * Constructs an empty {@code PasswordField} with the given label.
     * <p>
     * Using this constructor, any value previously set at the client-side is
     * cleared.
     *
     * @param label
     *            the text to set as the label
     */
    public PasswordField(String label) {
        this();
        setLabel(label);
    }

    /**
     * Constructs an empty {@code PasswordField} with the given label and
     * placeholder text.
     * <p>
     * Using this constructor, any value previously set at the client-side is
     * cleared.
     *
     * @param label
     *            the text to set as the label
     * @param placeholder
     *            the placeholder text to set
     */
    public PasswordField(String label, String placeholder) {
        this(label);
        setPlaceholder(placeholder);
    }

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
     * @return whether the element should have input focus when the page loads
     */
    public boolean isAutofocus() {
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
     * @return this instance, for method chaining
     */
    public PasswordField setAutofocus(boolean autofocus) {
        getElement().setProperty("autofocus", autofocus);
        return get();
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * If true, the element currently has focus.
     * <p>
     * This property is not synchronized automatically from the client side, so
     * the returned value may not be the same as in client side.
     * </p>
     *
     * @return whether an element has focus
     */
    public boolean isFocused() {
        return getElement().getProperty("focused", false);
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
     * @return whether an element is disabled
     */
    public boolean isDisabled() {
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
     * @return this instance, for method chaining
     */
    public PasswordField setDisabled(boolean disabled) {
        getElement().setProperty("disabled", disabled);
        return get();
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * Error to show when the input value is invalid.
     * <p>
     * This property is not synchronized automatically from the client side, so
     * the returned value may not be the same as in client side.
     * </p>
     *
     * @return error message from the component, if any.
     */
    public String getErrorMessage() {
        return getElement().getProperty("errorMessage");
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * Error to show when the input value is invalid.
     * </p>
     *
     * @param errorMessage
     *            the String value to set
     * @return this instance, for method chaining
     */
    public PasswordField setErrorMessage(String errorMessage) {
        getElement().setProperty("errorMessage",
                errorMessage == null ? "" : errorMessage);
        return get();
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
     * @return component label value
     */
    public String getLabel() {
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
     * @return this instance, for method chaining
     */
    public PasswordField setLabel(String label) {
        getElement().setProperty("label", label == null ? "" : label);
        return get();
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * Maximum number of characters (in Unicode code points) that the user can
     * enter.
     * <p>
     * This property is not synchronized automatically from the client side, so
     * the returned value may not be the same as in client side.
     * </p>
     *
     * @return max number of characters to be entered
     */
    public double getMaxlength() {
        return getElement().getProperty("maxlength", 0.0);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * Maximum number of characters (in Unicode code points) that the user can
     * enter.
     * </p>
     *
     * @param maxlength
     *            the double value to set
     * @return this instance, for method chaining
     */
    public PasswordField setMaxlength(double maxlength) {
        getElement().setProperty("maxlength", maxlength);
        return get();
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * Minimum number of characters (in Unicode code points) that the user can
     * enter.
     * <p>
     * This property is not synchronized automatically from the client side, so
     * the returned value may not be the same as in client side.
     * </p>
     *
     * @return minimum number of characters to be entered
     */
    public double getMinlength() {
        return getElement().getProperty("minlength", 0.0);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * Minimum number of characters (in Unicode code points) that the user can
     * enter.
     * </p>
     *
     * @param minlength
     *            the double value to set
     * @return this instance, for method chaining
     */
    public PasswordField setMinlength(double minlength) {
        getElement().setProperty("minlength", minlength);
        return get();
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * The name of the control, which is submitted with the form data.
     * <p>
     * This property is not synchronized automatically from the client side, so
     * the returned value may not be the same as in client side.
     * </p>
     *
     * @return the name of the control, which is submitted with the form data.
     */
    public String getName() {
        return getElement().getProperty("name");
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * The name of the control, which is submitted with the form data.
     * </p>
     *
     * @param name
     *            the String value to set
     * @return this instance, for method chaining
     */
    public PasswordField setName(String name) {
        getElement().setProperty("name", name == null ? "" : name);
        return get();
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * A regular expression that the value is checked against. The pattern must
     * match the entire value, not just some subset.
     * <p>
     * This property is not synchronized automatically from the client side, so
     * the returned value may not be the same as in client side.
     * </p>
     *
     * @return the regex patten that input is checked against
     */
    public String getPattern() {
        return getElement().getProperty("pattern");
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * A regular expression that the value is checked against. The pattern must
     * match the entire value, not just some subset.
     * </p>
     *
     * @param pattern
     *            the String value to set
     * @return this instance, for method chaining
     */
    public PasswordField setPattern(String pattern) {
        getElement().setProperty("pattern", pattern == null ? "" : pattern);
        return get();
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * A hint to the user of what can be entered in the control.
     * <p>
     * This property is not synchronized automatically from the client side, so
     * the returned value may not be the same as in client side.
     * </p>
     *
     * @return the hint to the user of what can be entered in the control.
     */
    public String getPlaceholder() {
        return getElement().getProperty("placeholder");
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * A hint to the user of what can be entered in the control.
     * </p>
     *
     * @param placeholder
     *            the String value to set
     * @return this instance, for method chaining
     */
    public PasswordField setPlaceholder(String placeholder) {
        getElement().setProperty("placeholder",
                placeholder == null ? "" : placeholder);
        return get();
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * Specifies that the user must fill in a value.
     * <p>
     * This property is not synchronized automatically from the client side, so
     * the returned value may not be the same as in client side.
     * </p>
     *
     * @return whether the user is required to fill the value of the component
     */
    public boolean isRequired() {
        return getElement().getProperty("required", false);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * Specifies that the user must fill in a value.
     * </p>
     *
     * @param required
     *            the boolean value to set
     * @return this instance, for method chaining
     */
    public PasswordField setRequired(boolean required) {
        getElement().setProperty("required", required);
        return get();
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * Message to show to the user when validation fails.
     * <p>
     * This property is not synchronized automatically from the client side, so
     * the returned value may not be the same as in client side.
     * </p>
     *
     * @return message to show to the user when validation fails
     */
    public String getTitle() {
        return getElement().getProperty("title");
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * Message to show to the user when validation fails.
     * </p>
     *
     * @param title
     *            the String value to set
     * @return this instance, for method chaining
     */
    public PasswordField setTitle(String title) {
        getElement().setProperty("title", title == null ? "" : title);
        return get();
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * The initial value of the control. It can be used for two-way data
     * binding.
     * <p>
     * This property is synchronized automatically from client side when a
     * 'value-changed' event happens.
     * </p>
     */
    @Synchronize(property = "value", value = "value-changed")
    @Override
    public String getValue() {
        return getElement().getProperty("value");
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * The initial value of the control. It can be used for two-way data
     * binding.
     * </p>
     *
     * @param value
     *            the String value to set
     * @return this instance, for method chaining
     */
    @Override
    public PasswordField setValue(String value) {
        getElement().setProperty("value", value == null ? "" : value);
        return get();
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * This property is set to true when the control value invalid.
     * <p>
     * This property is synchronized automatically from client side when a
     * 'invalid-changed' event happens.
     * </p>
     *
     * @return whether the user input is invalid or not
     */
    @Synchronize(property = "invalid", value = "invalid-changed")
    public boolean isInvalid() {
        return getElement().getProperty("invalid", false);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * This property is set to true when the control value invalid.
     * </p>
     *
     * @param invalid
     *            the boolean value to set
     * @return this instance, for method chaining
     */
    public PasswordField setInvalid(boolean invalid) {
        getElement().setProperty("invalid", invalid);
        return get();
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * A read-only property indicating whether this input has a non empty value.
     * It can be used for example in styling of the component.
     * <p>
     * This property is not synchronized automatically from the client side, so
     * the returned value may not be the same as in client side.
     * </p>
     *
     * @return whether a non empty value was input
     */
    public boolean hasValue() {
        return getElement().getProperty("hasValue", false);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * When set to true, user is prevented from typing a value that conflicts
     * with the given {@code pattern}.
     * <p>
     * This property is not synchronized automatically from the client side, so
     * the returned value may not be the same as in client side.
     * </p>
     *
     * @return whether user can enter invalid text into a component
     */
    public boolean isPreventInvalidInput() {
        return getElement().getProperty("preventInvalidInput", false);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * When set to true, user is prevented from typing a value that conflicts
     * with the given {@code pattern}.
     * </p>
     *
     * @param preventInvalidInput
     *            the boolean value to set
     * @return this instance, for method chaining
     */
    public PasswordField setPreventInvalidInput(boolean preventInvalidInput) {
        getElement().setProperty("preventInvalidInput", preventInvalidInput);
        return get();
    }

    /**
     * @param prop
     *            Missing documentation!
     * @param oldVal
     *            Missing documentation!
     * @param newVal
     *            Missing documentation!
     */
    protected void attributeChangedCallback(JsonObject prop, JsonObject oldVal,
            JsonObject newVal) {
        getElement().callFunction("attributeChangedCallback", prop, oldVal,
                newVal);
    }

    @DomEvent("iron-form-element-register")
    public static class IronFormElementRegisterEvent<R extends GeneratedVaadinPasswordField<R>>
            extends ComponentEvent<R> {
        public IronFormElementRegisterEvent(R source, boolean fromClient) {
            super(source, fromClient);
        }
    }

    public Registration addIronFormElementRegisterListener(
            ComponentEventListener<IronFormElementRegisterEvent<PasswordField>> listener) {
        return addListener(IronFormElementRegisterEvent.class,
                (ComponentEventListener) listener);
    }

    @DomEvent("iron-form-element-unregister")
    public static class IronFormElementUnregisterEvent<R extends GeneratedVaadinPasswordField<R>>
            extends ComponentEvent<R> {
        public IronFormElementUnregisterEvent(R source, boolean fromClient) {
            super(source, fromClient);
        }
    }

    public Registration addIronFormElementUnregisterListener(
            ComponentEventListener<IronFormElementUnregisterEvent<PasswordField>> listener) {
        return addListener(IronFormElementUnregisterEvent.class,
                (ComponentEventListener) listener);
    }

    @DomEvent("invalid-changed")
    public static class InvalidChangeEvent<R extends GeneratedVaadinPasswordField<R>>
            extends ComponentEvent<R> {
        public InvalidChangeEvent(R source, boolean fromClient) {
            super(source, fromClient);
        }
    }

    public Registration addInvalidChangeListener(
            ComponentEventListener<InvalidChangeEvent<PasswordField>> listener) {
        return addListener(InvalidChangeEvent.class,
                (ComponentEventListener) listener);
    }

    @DomEvent("has-value-changed")
    public static class HasValueChangeEvent<R extends GeneratedVaadinPasswordField<R>>
            extends ComponentEvent<R> {
        public HasValueChangeEvent(R source, boolean fromClient) {
            super(source, fromClient);
        }
    }

    /**
     * Adds the given components as children of this component at the slot
     * 'prefix'.
     *
     * @param components
     *            The components to add.
     * @see <a href=
     *      "https://developer.mozilla.org/en-US/docs/Web/HTML/Element/slot">MDN
     *      page about slots</a>
     * @see <a href=
     *      "https://html.spec.whatwg.org/multipage/scripting.html#the-slot-element">Spec
     *      website about slots</a>
     * @return this instance, for method chaining
     */
    public PasswordField addToPrefix(Component... components) {
        for (Component component : components) {
            component.getElement().setAttribute("slot", "prefix");
            getElement().appendChild(component.getElement());
        }
        return get();
    }

    /**
     * Adds the given components as children of this component at the slot
     * 'suffix'.
     *
     * @param components
     *            The components to add.
     * @see <a href=
     *      "https://developer.mozilla.org/en-US/docs/Web/HTML/Element/slot">MDN
     *      page about slots</a>
     * @see <a href=
     *      "https://html.spec.whatwg.org/multipage/scripting.html#the-slot-element">Spec
     *      website about slots</a>
     * @return this instance, for method chaining
     */
    public PasswordField addToSuffix(Component... components) {
        for (Component component : components) {
            component.getElement().setAttribute("slot", "suffix");
            getElement().appendChild(component.getElement());
        }
        return get();
    }

    /**
     * Removes the given child components from this component.
     *
     * @param components
     *            The components to remove.
     * @throws IllegalArgumentException
     *             if any of the components is not a child of this component.
     */
    public void remove(Component... components) {
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
     * this component using the {@code Element} API.
     */
    public void removeAll() {
        getElement().getChildren()
                .forEach(child -> child.removeAttribute("slot"));
        getElement().removeAllChildren();
    }

    @Override
    public String getEmptyValue() {
        return "";
    }

    @Override
    public boolean isEmpty() {
        return !hasValue();
    }
}
