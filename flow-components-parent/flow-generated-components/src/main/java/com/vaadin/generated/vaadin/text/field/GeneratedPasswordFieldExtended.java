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

package com.vaadin.generated.vaadin.text.field;

import com.vaadin.annotations.DomEvent;
import com.vaadin.annotations.Synchronize;
import com.vaadin.components.NotSupported;
import com.vaadin.components.data.HasValue;
import com.vaadin.flow.event.ComponentEventListener;
import com.vaadin.shared.Registration;
import com.vaadin.ui.Component;
import com.vaadin.ui.ComponentEvent;
import com.vaadin.ui.Focusable;

import elemental.json.JsonObject;

/**
 * A class that fills in missing methods that are not generated due to
 * <a href="https://github.com/vaadin/flow/issues/2197">this issue</a>.
 *
 * @author Vaadin Ltd.
 * @see <a href="https://github.com/vaadin/flow/issues/2197">Corresponding issue</a>
 */
public class GeneratedPasswordFieldExtended<R extends GeneratedVaadinPasswordField<R>>
        extends GeneratedVaadinPasswordField<R>
        implements Focusable<R>, HasValue<R, String> {

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
    public R setAutofocus(boolean autofocus) {
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
    public R setDisabled(boolean disabled) {
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
    public R setErrorMessage(String errorMessage) {
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
    public R setLabel(String label) {
        getElement().setProperty("label", label == null ? "" : label);
        return get();
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * Identifies a list of pre-defined options to suggest to the user. The
     * value must be the id of a <datalist> element in the same document.
     * <p>
     * This property is not synchronized automatically from the client side, so
     * the returned value may not be the same as in client side.
     * </p>
     */
    public String getList() {
        return getElement().getProperty("list");
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * Identifies a list of pre-defined options to suggest to the user. The
     * value must be the id of a <datalist> element in the same document.
     * </p>
     *
     * @param list
     *            the String value to set
     * @return this instance, for method chaining
     */
    public R setList(String list) {
        getElement().setProperty("list", list == null ? "" : list);
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
    public R setMaxlength(double maxlength) {
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
    public R setMinlength(double minlength) {
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
    public R setName(String name) {
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
    public R setPattern(String pattern) {
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
    public R setPlaceholder(String placeholder) {
        getElement().setProperty("placeholder",
                placeholder == null ? "" : placeholder);
        return get();
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * This attribute indicates that the user cannot modify the value of the
     * control.
     * <p>
     * This property is not synchronized automatically from the client side, so
     * the returned value may not be the same as in client side.
     * </p>
     */
    public boolean isReadonly() {
        return getElement().getProperty("readonly", false);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * This attribute indicates that the user cannot modify the value of the
     * control.
     * </p>
     *
     * @param readonly
     *            the boolean value to set
     * @return this instance, for method chaining
     */
    public R setReadonly(boolean readonly) {
        getElement().setProperty("readonly", readonly);
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
    public R setRequired(boolean required) {
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
    public R setTitle(String title) {
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
    public R setValue(String value) {
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
    public R setInvalid(boolean invalid) {
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
    public R setPreventInvalidInput(boolean preventInvalidInput) {
        getElement().setProperty("preventInvalidInput", preventInvalidInput);
        return get();
    }

    public void connectedCallback() {
        getElement().callFunction("connectedCallback");
    }

    public void disconnectedCallback() {
        getElement().callFunction("disconnectedCallback");
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * Returns true if {@code value} is valid. {@code <iron-form>} uses this to
     * check the validity or all its elements.
     * </p>
     *
     * @return It would return a boolean
     */
    @NotSupported
    protected void validate() {
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * Returns true if the current input value satisfies all constraints (if
     * any)
     * </p>
     */
    public void checkValidity() {
        getElement().callFunction("checkValidity");
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
            ComponentEventListener<IronFormElementRegisterEvent<R>> listener) {
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
            ComponentEventListener<IronFormElementUnregisterEvent<R>> listener) {
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
            ComponentEventListener<InvalidChangeEvent<R>> listener) {
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

    public Registration addHasValueChangeListener(
            ComponentEventListener<HasValueChangeEvent<R>> listener) {
        return addListener(HasValueChangeEvent.class,
                (ComponentEventListener) listener);
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
    public R addToPrefix(Component... components) {
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
    public R addToSuffix(Component... components) {
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
}
