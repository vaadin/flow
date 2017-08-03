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

import com.vaadin.ui.Component;
import com.vaadin.ui.ComponentSupplier;
import com.vaadin.ui.HasStyle;
import com.vaadin.ui.Focusable;
import javax.annotation.Generated;
import com.vaadin.annotations.Tag;
import com.vaadin.annotations.HtmlImport;
import com.vaadin.annotations.Synchronize;
import com.vaadin.components.data.HasValue;
import com.vaadin.components.NotSupported;
import elemental.json.JsonObject;
import com.vaadin.annotations.DomEvent;
import com.vaadin.ui.ComponentEvent;
import com.vaadin.flow.event.ComponentEventListener;
import com.vaadin.shared.Registration;

/**
 * Description copied from corresponding location in WebComponent:
 * 
 * {@code <vaadin-text-field>} is a Polymer 2 element for text field control in
 * forms.
 * 
 * {@code }`html <vaadin-text-field label="First Name"> </vaadin-text-field>
 * {@code }`
 * 
 * ### Styling
 * 
 * The following shadow DOM parts are available for styling:
 * 
 * Part name | Description ----------------|---------------- {@code label} | The
 * label element {@code value} | The input element {@code error-message} | The
 * error message element {@code input-field} | The element that wraps prefix,
 * value and suffix
 */
@Generated({
		"Generator: com.vaadin.generator.ComponentGenerator#0.1.16-SNAPSHOT",
		"WebComponent: Vaadin.TextFieldElement#1.1.0-alpha3",
		"Flow#0.1.16-SNAPSHOT"})
@Tag("vaadin-text-field")
@HtmlImport("frontend://bower_components/vaadin-text-field/vaadin-text-field.html")
public class GeneratedVaadinTextField<R extends GeneratedVaadinTextField<R>>
		extends
			Component
		implements
			ComponentSupplier<R>,
			HasStyle,
			Focusable<R>,
			HasValue<R, String> {

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Specify that this control should have input focus when the page loads.
	 * <p>
	 * This property is not synchronized automatically from the client side, so
	 * the returned value may not be the same as in client side.
	 */
	public boolean isAutofocus() {
		return getElement().getProperty("autofocus", false);
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Specify that this control should have input focus when the page loads.
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
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * If true, the element currently has focus.
	 * <p>
	 * This property is not synchronized automatically from the client side, so
	 * the returned value may not be the same as in client side.
	 */
	public boolean isFocused() {
		return getElement().getProperty("focused", false);
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * If true, the user cannot interact with this element.
	 * <p>
	 * This property is not synchronized automatically from the client side, so
	 * the returned value may not be the same as in client side.
	 */
	public boolean isDisabled() {
		return getElement().getProperty("disabled", false);
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * If true, the user cannot interact with this element.
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
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Whether the value of the control can be automatically completed by the
	 * browser. List of available options at:
	 * https://developer.mozilla.org/en/docs
	 * /Web/HTML/Element/input#attr-autocomplete
	 * <p>
	 * This property is not synchronized automatically from the client side, so
	 * the returned value may not be the same as in client side.
	 */
	public String getAutocomplete() {
		return getElement().getProperty("autocomplete");
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Whether the value of the control can be automatically completed by the
	 * browser. List of available options at:
	 * https://developer.mozilla.org/en/docs
	 * /Web/HTML/Element/input#attr-autocomplete
	 * 
	 * @param autocomplete
	 *            the String value to set
	 * @return this instance, for method chaining
	 */
	public R setAutocomplete(java.lang.String autocomplete) {
		getElement().setProperty("autocomplete",
				autocomplete == null ? "" : autocomplete);
		return get();
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * This is a property supported by Safari that is used to control whether
	 * autocorrection should be enabled when the user is entering/editing the
	 * text. Possible values are: on: Enable autocorrection. off: Disable
	 * autocorrection.
	 * <p>
	 * This property is not synchronized automatically from the client side, so
	 * the returned value may not be the same as in client side.
	 */
	public String getAutocorrect() {
		return getElement().getProperty("autocorrect");
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * This is a property supported by Safari that is used to control whether
	 * autocorrection should be enabled when the user is entering/editing the
	 * text. Possible values are: on: Enable autocorrection. off: Disable
	 * autocorrection.
	 * 
	 * @param autocorrect
	 *            the String value to set
	 * @return this instance, for method chaining
	 */
	public R setAutocorrect(java.lang.String autocorrect) {
		getElement().setProperty("autocorrect",
				autocorrect == null ? "" : autocorrect);
		return get();
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Error to show when the input value is invalid.
	 * <p>
	 * This property is not synchronized automatically from the client side, so
	 * the returned value may not be the same as in client side.
	 */
	public String getErrorMessage() {
		return getElement().getProperty("errorMessage");
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Error to show when the input value is invalid.
	 * 
	 * @param errorMessage
	 *            the String value to set
	 * @return this instance, for method chaining
	 */
	public R setErrorMessage(java.lang.String errorMessage) {
		getElement().setProperty("errorMessage",
				errorMessage == null ? "" : errorMessage);
		return get();
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * String used for the label element.
	 * <p>
	 * This property is not synchronized automatically from the client side, so
	 * the returned value may not be the same as in client side.
	 */
	public String getLabel() {
		return getElement().getProperty("label");
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * String used for the label element.
	 * 
	 * @param label
	 *            the String value to set
	 * @return this instance, for method chaining
	 */
	public R setLabel(java.lang.String label) {
		getElement().setProperty("label", label == null ? "" : label);
		return get();
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Identifies a list of pre-defined options to suggest to the user. The
	 * value must be the id of a <datalist> element in the same document.
	 * <p>
	 * This property is not synchronized automatically from the client side, so
	 * the returned value may not be the same as in client side.
	 */
	public String getList() {
		return getElement().getProperty("list");
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Identifies a list of pre-defined options to suggest to the user. The
	 * value must be the id of a <datalist> element in the same document.
	 * 
	 * @param list
	 *            the String value to set
	 * @return this instance, for method chaining
	 */
	public R setList(java.lang.String list) {
		getElement().setProperty("list", list == null ? "" : list);
		return get();
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Maximum number of characters (in Unicode code points) that the user can
	 * enter.
	 * <p>
	 * This property is not synchronized automatically from the client side, so
	 * the returned value may not be the same as in client side.
	 */
	public double getMaxlength() {
		return getElement().getProperty("maxlength", 0.0);
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Maximum number of characters (in Unicode code points) that the user can
	 * enter.
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
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Minimum number of characters (in Unicode code points) that the user can
	 * enter.
	 * <p>
	 * This property is not synchronized automatically from the client side, so
	 * the returned value may not be the same as in client side.
	 */
	public double getMinlength() {
		return getElement().getProperty("minlength", 0.0);
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Minimum number of characters (in Unicode code points) that the user can
	 * enter.
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
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * The name of the control, which is submitted with the form data.
	 * <p>
	 * This property is not synchronized automatically from the client side, so
	 * the returned value may not be the same as in client side.
	 */
	public String getName() {
		return getElement().getProperty("name");
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * The name of the control, which is submitted with the form data.
	 * 
	 * @param name
	 *            the String value to set
	 * @return this instance, for method chaining
	 */
	public R setName(java.lang.String name) {
		getElement().setProperty("name", name == null ? "" : name);
		return get();
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * A regular expression that the value is checked against. The pattern must
	 * match the entire value, not just some subset.
	 * <p>
	 * This property is not synchronized automatically from the client side, so
	 * the returned value may not be the same as in client side.
	 */
	public String getPattern() {
		return getElement().getProperty("pattern");
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * A regular expression that the value is checked against. The pattern must
	 * match the entire value, not just some subset.
	 * 
	 * @param pattern
	 *            the String value to set
	 * @return this instance, for method chaining
	 */
	public R setPattern(java.lang.String pattern) {
		getElement().setProperty("pattern", pattern == null ? "" : pattern);
		return get();
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * A hint to the user of what can be entered in the control.
	 * <p>
	 * This property is not synchronized automatically from the client side, so
	 * the returned value may not be the same as in client side.
	 */
	public String getPlaceholder() {
		return getElement().getProperty("placeholder");
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * A hint to the user of what can be entered in the control.
	 * 
	 * @param placeholder
	 *            the String value to set
	 * @return this instance, for method chaining
	 */
	public R setPlaceholder(java.lang.String placeholder) {
		getElement().setProperty("placeholder",
				placeholder == null ? "" : placeholder);
		return get();
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * This attribute indicates that the user cannot modify the value of the
	 * control.
	 * <p>
	 * This property is not synchronized automatically from the client side, so
	 * the returned value may not be the same as in client side.
	 */
	public boolean isReadonly() {
		return getElement().getProperty("readonly", false);
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * This attribute indicates that the user cannot modify the value of the
	 * control.
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
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Specifies that the user must fill in a value.
	 * <p>
	 * This property is not synchronized automatically from the client side, so
	 * the returned value may not be the same as in client side.
	 */
	public boolean isRequired() {
		return getElement().getProperty("required", false);
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Specifies that the user must fill in a value.
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
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Message to show to the user when validation fails.
	 * <p>
	 * This property is not synchronized automatically from the client side, so
	 * the returned value may not be the same as in client side.
	 */
	public String getTitle() {
		return getElement().getProperty("title");
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Message to show to the user when validation fails.
	 * 
	 * @param title
	 *            the String value to set
	 * @return this instance, for method chaining
	 */
	public R setTitle(java.lang.String title) {
		getElement().setProperty("title", title == null ? "" : title);
		return get();
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * The initial value of the control. It can be used for two-way data
	 * binding.
	 * <p>
	 * This property is synchronized automatically from client side when a
	 * 'value-changed' event happens.
	 */
	@Synchronize(property = "value", value = "value-changed")
	@Override
	public String getValue() {
		return getElement().getProperty("value");
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * The initial value of the control. It can be used for two-way data
	 * binding.
	 * 
	 * @param value
	 *            the String value to set
	 * @return this instance, for method chaining
	 */
	@Override
	public R setValue(java.lang.String value) {
		getElement().setProperty("value", value == null ? "" : value);
		return get();
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * This property is set to true when the control value invalid.
	 * <p>
	 * This property is synchronized automatically from client side when a
	 * 'invalid-changed' event happens.
	 */
	@Synchronize(property = "invalid", value = "invalid-changed")
	public boolean isInvalid() {
		return getElement().getProperty("invalid", false);
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * This property is set to true when the control value invalid.
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
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * A read-only property indicating whether this input has a non empty value.
	 * It can be used for example in styling of the component.
	 * <p>
	 * This property is not synchronized automatically from the client side, so
	 * the returned value may not be the same as in client side.
	 */
	public boolean hasValue() {
		return getElement().getProperty("hasValue", false);
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * When set to true, user is prevented from typing a value that conflicts
	 * with the given {@code pattern}.
	 * <p>
	 * This property is not synchronized automatically from the client side, so
	 * the returned value may not be the same as in client side.
	 */
	public boolean isPreventInvalidInput() {
		return getElement().getProperty("preventInvalidInput", false);
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * When set to true, user is prevented from typing a value that conflicts
	 * with the given {@code pattern}.
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
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Returns true if {@code value} is valid. {@code <iron-form>} uses this to
	 * check the validity or all its elements.
	 * 
	 * @return It would return a boolean
	 */
	@NotSupported
	protected void validate() {
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Returns true if the current input value satisfies all constraints (if
	 * any)
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
	protected void attributeChangedCallback(JsonObject prop,
			elemental.json.JsonObject oldVal, elemental.json.JsonObject newVal) {
		getElement().callFunction("attributeChangedCallback", prop, oldVal,
				newVal);
	}

	@DomEvent("iron-form-element-register")
	public static class IronFormElementRegisterEvent<R extends GeneratedVaadinTextField<R>>
			extends
				ComponentEvent<R> {
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
	public static class IronFormElementUnregisterEvent<R extends GeneratedVaadinTextField<R>>
			extends
				ComponentEvent<R> {
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
	public static class InvalidChangeEvent<R extends GeneratedVaadinTextField<R>>
			extends
				ComponentEvent<R> {
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
	public static class HasValueChangeEvent<R extends GeneratedVaadinTextField<R>>
			extends
				ComponentEvent<R> {
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
	 * @see <a
	 *      href="https://developer.mozilla.org/en-US/docs/Web/HTML/Element/slot">MDN
	 *      page about slots</a>
	 * @see <a
	 *      href="https://html.spec.whatwg.org/multipage/scripting.html#the-slot-element">Spec
	 *      website about slots</a>
	 * @return this instance, for method chaining
	 */
	public R addToPrefix(com.vaadin.ui.Component... components) {
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
	 * @see <a
	 *      href="https://developer.mozilla.org/en-US/docs/Web/HTML/Element/slot">MDN
	 *      page about slots</a>
	 * @see <a
	 *      href="https://html.spec.whatwg.org/multipage/scripting.html#the-slot-element">Spec
	 *      website about slots</a>
	 * @return this instance, for method chaining
	 */
	public R addToSuffix(com.vaadin.ui.Component... components) {
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
	public void remove(com.vaadin.ui.Component... components) {
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
	public void removeAll() {
		getElement().getChildren().forEach(
				child -> child.removeAttribute("slot"));
		getElement().removeAllChildren();
	}
}