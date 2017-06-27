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
package com.vaadin.components.vaadin.text.field;

import com.vaadin.ui.Component;
import javax.annotation.Generated;
import com.vaadin.annotations.Tag;
import com.vaadin.annotations.HtmlImport;
import elemental.json.JsonObject;
import com.vaadin.annotations.DomEvent;
import com.vaadin.ui.ComponentEvent;
import com.vaadin.flow.event.ComponentEventListener;
import com.vaadin.shared.Registration;

/**
 * Description copied from corresponding location in WebComponent:
 * 
 * {@code <vaadin-text-field>} is a Polymer element for text field control in
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
		"Generator: com.vaadin.generator.ComponentGenerator#0.1.11-SNAPSHOT",
		"WebComponent: Vaadin.TextFieldElement#null", "Flow#0.1.11-SNAPSHOT"})
@Tag("vaadin-text-field")
@HtmlImport("frontend://bower_components/vaadin-text-field/vaadin-text-field.html")
public class VaadinTextFieldElement<R extends VaadinTextFieldElement<R>>
		extends
			Component {

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Specify that this control should have input focus when the page loads.
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
	 * @return This instance, for method chaining.
	 */
	public R setAutofocus(boolean autofocus) {
		getElement().setProperty("autofocus", autofocus);
		return getSelf();
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * If true, the element currently has focus.
	 */
	public boolean isFocused() {
		return getElement().getProperty("focused", false);
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * If true, the element currently has focus.
	 * 
	 * @param focused
	 * @return This instance, for method chaining.
	 */
	public R setFocused(boolean focused) {
		getElement().setProperty("focused", focused);
		return getSelf();
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * If true, the user cannot interact with this element.
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
	 * @return This instance, for method chaining.
	 */
	public R setDisabled(boolean disabled) {
		getElement().setProperty("disabled", disabled);
		return getSelf();
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Whether the value of the control can be automatically completed by the
	 * browser. List of available options at:
	 * https://developer.mozilla.org/en/docs
	 * /Web/HTML/Element/input#attr-autocomplete
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
	 * @return This instance, for method chaining.
	 */
	public R setAutocomplete(java.lang.String autocomplete) {
		getElement().setProperty("autocomplete",
				autocomplete == null ? "" : autocomplete);
		return getSelf();
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * This is a property supported by Safari that is used to control whether
	 * autocorrection should be enabled when the user is entering/editing the
	 * text. Possible values are: on: Enable autocorrection. off: Disable
	 * autocorrection.
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
	 * @return This instance, for method chaining.
	 */
	public R setAutocorrect(java.lang.String autocorrect) {
		getElement().setProperty("autocorrect",
				autocorrect == null ? "" : autocorrect);
		return getSelf();
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Error to show when the input value is invalid.
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
	 * @return This instance, for method chaining.
	 */
	public R setErrorMessage(java.lang.String errorMessage) {
		getElement().setProperty("errorMessage",
				errorMessage == null ? "" : errorMessage);
		return getSelf();
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * String used for the label element.
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
	 * @return This instance, for method chaining.
	 */
	public R setLabel(java.lang.String label) {
		getElement().setProperty("label", label == null ? "" : label);
		return getSelf();
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Identifies a list of pre-defined options to suggest to the user. The
	 * value must be the id of a <datalist> element in the same document.
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
	 * @return This instance, for method chaining.
	 */
	public R setList(java.lang.String list) {
		getElement().setProperty("list", list == null ? "" : list);
		return getSelf();
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Maximum number of characters (in Unicode code points) that the user can
	 * enter.
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
	 * @return This instance, for method chaining.
	 */
	public R setMaxlength(double maxlength) {
		getElement().setProperty("maxlength", maxlength);
		return getSelf();
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Minimum number of characters (in Unicode code points) that the user can
	 * enter.
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
	 * @return This instance, for method chaining.
	 */
	public R setMinlength(double minlength) {
		getElement().setProperty("minlength", minlength);
		return getSelf();
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * The name of the control, which is submitted with the form data.
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
	 * @return This instance, for method chaining.
	 */
	public R setName(java.lang.String name) {
		getElement().setProperty("name", name == null ? "" : name);
		return getSelf();
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * A regular expression that the value is checked against. The pattern must
	 * match the entire value, not just some subset.
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
	 * @return This instance, for method chaining.
	 */
	public R setPattern(java.lang.String pattern) {
		getElement().setProperty("pattern", pattern == null ? "" : pattern);
		return getSelf();
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * A hint to the user of what can be entered in the control.
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
	 * @return This instance, for method chaining.
	 */
	public R setPlaceholder(java.lang.String placeholder) {
		getElement().setProperty("placeholder",
				placeholder == null ? "" : placeholder);
		return getSelf();
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * This attribute indicates that the user cannot modify the value of the
	 * control.
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
	 * @return This instance, for method chaining.
	 */
	public R setReadonly(boolean readonly) {
		getElement().setProperty("readonly", readonly);
		return getSelf();
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Specifies that the user must fill in a value.
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
	 * @return This instance, for method chaining.
	 */
	public R setRequired(boolean required) {
		getElement().setProperty("required", required);
		return getSelf();
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Message to show to the user when validation fails.
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
	 * @return This instance, for method chaining.
	 */
	public R setTitle(java.lang.String title) {
		getElement().setProperty("title", title == null ? "" : title);
		return getSelf();
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * The initial value of the control. It can be used for two-way data
	 * binding.
	 */
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
	 * @return This instance, for method chaining.
	 */
	public R setValue(java.lang.String value) {
		getElement().setProperty("value", value == null ? "" : value);
		return getSelf();
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * This property is set to true when the control value invalid.
	 */
	public boolean isInvalid() {
		return getElement().getProperty("invalid", false);
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * This property is set to true when the control value invalid.
	 * 
	 * @param invalid
	 * @return This instance, for method chaining.
	 */
	public R setInvalid(boolean invalid) {
		getElement().setProperty("invalid", invalid);
		return getSelf();
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * A read-only property indicating whether this input has a non empty value.
	 * It can be used for example in styling of the component.
	 */
	public boolean isHasValue() {
		return getElement().getProperty("hasValue", false);
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * A read-only property indicating whether this input has a non empty value.
	 * It can be used for example in styling of the component.
	 * 
	 * @param hasValue
	 * @return This instance, for method chaining.
	 */
	public R setHasValue(boolean hasValue) {
		getElement().setProperty("hasValue", hasValue);
		return getSelf();
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * When set to true, user is prevented from typing a value that conflicts
	 * with the given {@code pattern}.
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
	 * @return This instance, for method chaining.
	 */
	public R setPreventInvalidInput(boolean preventInvalidInput) {
		getElement().setProperty("preventInvalidInput", preventInvalidInput);
		return getSelf();
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
	 */
	public void validate() {
		getElement().callFunction("validate");
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
	 * @param oldVal
	 * @param newVal
	 */
	public void attributeChangedCallback(JsonObject prop,
			elemental.json.JsonObject oldVal, elemental.json.JsonObject newVal) {
		getElement().callFunction("attributeChangedCallback", prop, oldVal,
				newVal);
	}

	@DomEvent("iron-form-element-register")
	public static class IronFormElementRegisterEvent
			extends
				ComponentEvent<VaadinTextFieldElement> {
		public IronFormElementRegisterEvent(VaadinTextFieldElement source,
				boolean fromClient) {
			super(source, fromClient);
		}
	}

	public Registration addIronFormElementRegisterListener(
			ComponentEventListener<IronFormElementRegisterEvent> listener) {
		return addListener(IronFormElementRegisterEvent.class, listener);
	}

	@DomEvent("iron-form-element-unregister")
	public static class IronFormElementUnregisterEvent
			extends
				ComponentEvent<VaadinTextFieldElement> {
		public IronFormElementUnregisterEvent(VaadinTextFieldElement source,
				boolean fromClient) {
			super(source, fromClient);
		}
	}

	public Registration addIronFormElementUnregisterListener(
			ComponentEventListener<IronFormElementUnregisterEvent> listener) {
		return addListener(IronFormElementUnregisterEvent.class, listener);
	}

	@DomEvent("value-changed")
	public static class ValueChangedEvent
			extends
				ComponentEvent<VaadinTextFieldElement> {
		public ValueChangedEvent(VaadinTextFieldElement source,
				boolean fromClient) {
			super(source, fromClient);
		}
	}

	public Registration addValueChangedListener(
			ComponentEventListener<ValueChangedEvent> listener) {
		return addListener(ValueChangedEvent.class, listener);
	}

	@DomEvent("invalid-changed")
	public static class InvalidChangedEvent
			extends
				ComponentEvent<VaadinTextFieldElement> {
		public InvalidChangedEvent(VaadinTextFieldElement source,
				boolean fromClient) {
			super(source, fromClient);
		}
	}

	public Registration addInvalidChangedListener(
			ComponentEventListener<InvalidChangedEvent> listener) {
		return addListener(InvalidChangedEvent.class, listener);
	}

	@DomEvent("has-value-changed")
	public static class HasValueChangedEvent
			extends
				ComponentEvent<VaadinTextFieldElement> {
		public HasValueChangedEvent(VaadinTextFieldElement source,
				boolean fromClient) {
			super(source, fromClient);
		}
	}

	public Registration addHasValueChangedListener(
			ComponentEventListener<HasValueChangedEvent> listener) {
		return addListener(HasValueChangedEvent.class, listener);
	}

	/**
	 * Gets the narrow typed reference to this object. Subclasses should
	 * override this method to support method chaining using the inherited type.
	 * 
	 * @return This object casted to its type.
	 */
	protected R getSelf() {
		return (R) this;
	}
}