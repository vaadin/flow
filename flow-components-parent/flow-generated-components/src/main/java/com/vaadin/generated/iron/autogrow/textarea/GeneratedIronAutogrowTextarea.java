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
package com.vaadin.generated.iron.autogrow.textarea;

import com.vaadin.ui.Component;
import com.vaadin.ui.HasStyle;
import javax.annotation.Generated;
import com.vaadin.annotations.Tag;
import com.vaadin.annotations.HtmlImport;
import com.vaadin.generated.iron.autogrow.textarea.GeneratedIronAutogrowTextarea;
import com.vaadin.annotations.Synchronize;
import elemental.json.JsonObject;
import com.vaadin.components.NotSupported;
import com.vaadin.annotations.DomEvent;
import com.vaadin.ui.ComponentEvent;
import com.vaadin.flow.event.ComponentEventListener;
import com.vaadin.shared.Registration;

/**
 * Description copied from corresponding location in WebComponent:
 * 
 * {@code iron-autogrow-textarea} is an element containing a textarea that grows
 * in height as more lines of input are entered. Unless an explicit height or
 * the {@code maxRows} property is set, it will never scroll.
 * 
 * Example:
 * 
 * <iron-autogrow-textarea></iron-autogrow-textarea>
 * 
 * ### Styling
 * 
 * The following custom properties and mixins are available for styling:
 * 
 * Custom property | Description | Default
 * ----------------|-------------|---------- {@code --iron-autogrow-textarea} |
 * Mixin applied to the textarea | {@code
 * {@code --iron-autogrow-textarea-placeholder} | Mixin applied to the textarea
 * placeholder | {@code
 */
@Generated({
		"Generator: com.vaadin.generator.ComponentGenerator#0.1.14-SNAPSHOT",
		"WebComponent: iron-autogrow-textarea#2.0.1", "Flow#0.1.14-SNAPSHOT"})
@Tag("iron-autogrow-textarea")
@HtmlImport("frontend://bower_components/iron-autogrow-textarea/iron-autogrow-textarea.html")
public class GeneratedIronAutogrowTextarea extends Component
		implements
			HasStyle {

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Name of the validator to use.
	 * <p>
	 * This property is not synchronized automatically from the client side, so
	 * the returned value may not be the same as in client side.
	 */
	public String getValidator() {
		return getElement().getProperty("validator");
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Name of the validator to use.
	 * 
	 * @param validator
	 *            the String value to set
	 * @return this instance, for method chaining
	 */
	public <R extends GeneratedIronAutogrowTextarea> R setValidator(
			java.lang.String validator) {
		getElement().setProperty("validator",
				validator == null ? "" : validator);
		return getSelf();
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * True if the last call to {@code validate} is invalid.
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
	 * True if the last call to {@code validate} is invalid.
	 * 
	 * @param invalid
	 *            the boolean value to set
	 * @return this instance, for method chaining
	 */
	public <R extends GeneratedIronAutogrowTextarea> R setInvalid(
			boolean invalid) {
		getElement().setProperty("invalid", invalid);
		return getSelf();
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * If true, the element currently has focus.
	 * <p>
	 * This property is synchronized automatically from client side when a
	 * 'focused-changed' event happens.
	 */
	@Synchronize(property = "focused", value = "focused-changed")
	public boolean isFocused() {
		return getElement().getProperty("focused", false);
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * If true, the user cannot interact with this element.
	 * <p>
	 * This property is synchronized automatically from client side when a
	 * 'disabled-changed' event happens.
	 */
	@Synchronize(property = "disabled", value = "disabled-changed")
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
	public <R extends GeneratedIronAutogrowTextarea> R setDisabled(
			boolean disabled) {
		getElement().setProperty("disabled", disabled);
		return getSelf();
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Use this property instead of {@code bind-value} for two-way data binding.
	 * <p>
	 * This property is synchronized automatically from client side when a
	 * 'value-changed' event happens.
	 */
	@Synchronize(property = "value", value = "value-changed")
	public String getValueString() {
		return getElement().getProperty("value");
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Use this property instead of {@code bind-value} for two-way data binding.
	 * <p>
	 * This property is synchronized automatically from client side when a
	 * 'value-changed' event happens.
	 */
	@Synchronize(property = "value", value = "value-changed")
	public double getValueNumber() {
		return getElement().getProperty("value", 0.0);
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Use this property instead of {@code bind-value} for two-way data binding.
	 * 
	 * @param value
	 *            the String value to set
	 * @return this instance, for method chaining
	 */
	public <R extends GeneratedIronAutogrowTextarea> R setValue(
			java.lang.String value) {
		getElement().setProperty("value", value == null ? "" : value);
		return getSelf();
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Use this property instead of {@code bind-value} for two-way data binding.
	 * 
	 * @param value
	 *            the double value to set
	 * @return this instance, for method chaining
	 */
	public <R extends GeneratedIronAutogrowTextarea> R setValue(double value) {
		getElement().setProperty("value", value);
		return getSelf();
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * This property is deprecated, and just mirrors {@code value}. Use
	 * {@code value} instead.
	 * <p>
	 * This property is not synchronized automatically from the client side, so
	 * the returned value may not be the same as in client side.
	 */
	public String getBindValueString() {
		return getElement().getProperty("bindValue");
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * This property is deprecated, and just mirrors {@code value}. Use
	 * {@code value} instead.
	 * <p>
	 * This property is not synchronized automatically from the client side, so
	 * the returned value may not be the same as in client side.
	 */
	public double getBindValueNumber() {
		return getElement().getProperty("bindValue", 0.0);
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * This property is deprecated, and just mirrors {@code value}. Use
	 * {@code value} instead.
	 * 
	 * @param bindValue
	 *            the String value to set
	 * @return this instance, for method chaining
	 */
	public <R extends GeneratedIronAutogrowTextarea> R setBindValue(
			java.lang.String bindValue) {
		getElement().setProperty("bindValue",
				bindValue == null ? "" : bindValue);
		return getSelf();
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * This property is deprecated, and just mirrors {@code value}. Use
	 * {@code value} instead.
	 * 
	 * @param bindValue
	 *            the double value to set
	 * @return this instance, for method chaining
	 */
	public <R extends GeneratedIronAutogrowTextarea> R setBindValue(
			double bindValue) {
		getElement().setProperty("bindValue", bindValue);
		return getSelf();
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * The initial number of rows.
	 * <p>
	 * This property is not synchronized automatically from the client side, so
	 * the returned value may not be the same as in client side.
	 */
	public double getRows() {
		return getElement().getProperty("rows", 0.0);
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * The initial number of rows.
	 * 
	 * @param rows
	 *            the double value to set
	 * @return this instance, for method chaining
	 */
	public <R extends GeneratedIronAutogrowTextarea> R setRows(double rows) {
		getElement().setProperty("rows", rows);
		return getSelf();
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * The maximum number of rows this element can grow to until it scrolls. 0
	 * means no maximum.
	 * <p>
	 * This property is not synchronized automatically from the client side, so
	 * the returned value may not be the same as in client side.
	 */
	public double getMaxRows() {
		return getElement().getProperty("maxRows", 0.0);
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * The maximum number of rows this element can grow to until it scrolls. 0
	 * means no maximum.
	 * 
	 * @param maxRows
	 *            the double value to set
	 * @return this instance, for method chaining
	 */
	public <R extends GeneratedIronAutogrowTextarea> R setMaxRows(double maxRows) {
		getElement().setProperty("maxRows", maxRows);
		return getSelf();
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Bound to the textarea's {@code autocomplete} attribute.
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
	 * Bound to the textarea's {@code autocomplete} attribute.
	 * 
	 * @param autocomplete
	 *            the String value to set
	 * @return this instance, for method chaining
	 */
	public <R extends GeneratedIronAutogrowTextarea> R setAutocomplete(
			java.lang.String autocomplete) {
		getElement().setProperty("autocomplete",
				autocomplete == null ? "" : autocomplete);
		return getSelf();
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Bound to the textarea's {@code autofocus} attribute.
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
	 * Bound to the textarea's {@code autofocus} attribute.
	 * 
	 * @param autofocus
	 *            the boolean value to set
	 * @return this instance, for method chaining
	 */
	public <R extends GeneratedIronAutogrowTextarea> R setAutofocus(
			boolean autofocus) {
		getElement().setProperty("autofocus", autofocus);
		return getSelf();
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Bound to the textarea's {@code inputmode} attribute.
	 * <p>
	 * This property is not synchronized automatically from the client side, so
	 * the returned value may not be the same as in client side.
	 */
	public String getInputmode() {
		return getElement().getProperty("inputmode");
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Bound to the textarea's {@code inputmode} attribute.
	 * 
	 * @param inputmode
	 *            the String value to set
	 * @return this instance, for method chaining
	 */
	public <R extends GeneratedIronAutogrowTextarea> R setInputmode(
			java.lang.String inputmode) {
		getElement().setProperty("inputmode",
				inputmode == null ? "" : inputmode);
		return getSelf();
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Bound to the textarea's {@code placeholder} attribute.
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
	 * Bound to the textarea's {@code placeholder} attribute.
	 * 
	 * @param placeholder
	 *            the String value to set
	 * @return this instance, for method chaining
	 */
	public <R extends GeneratedIronAutogrowTextarea> R setPlaceholder(
			java.lang.String placeholder) {
		getElement().setProperty("placeholder",
				placeholder == null ? "" : placeholder);
		return getSelf();
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Bound to the textarea's {@code readonly} attribute.
	 * <p>
	 * This property is not synchronized automatically from the client side, so
	 * the returned value may not be the same as in client side.
	 */
	public String getReadonly() {
		return getElement().getProperty("readonly");
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Bound to the textarea's {@code readonly} attribute.
	 * 
	 * @param readonly
	 *            the String value to set
	 * @return this instance, for method chaining
	 */
	public <R extends GeneratedIronAutogrowTextarea> R setReadonly(
			java.lang.String readonly) {
		getElement().setProperty("readonly", readonly == null ? "" : readonly);
		return getSelf();
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Set to true to mark the textarea as required.
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
	 * Set to true to mark the textarea as required.
	 * 
	 * @param required
	 *            the boolean value to set
	 * @return this instance, for method chaining
	 */
	public <R extends GeneratedIronAutogrowTextarea> R setRequired(
			boolean required) {
		getElement().setProperty("required", required);
		return getSelf();
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * The minimum length of the input value.
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
	 * The minimum length of the input value.
	 * 
	 * @param minlength
	 *            the double value to set
	 * @return this instance, for method chaining
	 */
	public <R extends GeneratedIronAutogrowTextarea> R setMinlength(
			double minlength) {
		getElement().setProperty("minlength", minlength);
		return getSelf();
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * The maximum length of the input value.
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
	 * The maximum length of the input value.
	 * 
	 * @param maxlength
	 *            the double value to set
	 * @return this instance, for method chaining
	 */
	public <R extends GeneratedIronAutogrowTextarea> R setMaxlength(
			double maxlength) {
		getElement().setProperty("maxlength", maxlength);
		return getSelf();
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Bound to the textarea's {@code aria-label} attribute.
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
	 * Bound to the textarea's {@code aria-label} attribute.
	 * 
	 * @param label
	 *            the String value to set
	 * @return this instance, for method chaining
	 */
	public <R extends GeneratedIronAutogrowTextarea> R setLabel(
			java.lang.String label) {
		getElement().setProperty("label", label == null ? "" : label);
		return getSelf();
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Returns the underlying textarea.
	 * <p>
	 * This property is not synchronized automatically from the client side, so
	 * the returned value may not be the same as in client side.
	 */
	public JsonObject getTextarea() {
		return (JsonObject) getElement().getPropertyRaw("textarea");
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Returns the underlying textarea.
	 * 
	 * @param textarea
	 *            the JsonObject value to set
	 * @return this instance, for method chaining
	 */
	public <R extends GeneratedIronAutogrowTextarea> R setTextarea(
			elemental.json.JsonObject textarea) {
		getElement().setPropertyJson("textarea", textarea);
		return getSelf();
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Returns textarea's selection start.
	 * <p>
	 * This property is not synchronized automatically from the client side, so
	 * the returned value may not be the same as in client side.
	 */
	public JsonObject getSelectionStart() {
		return (JsonObject) getElement().getPropertyRaw("selectionStart");
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Returns textarea's selection end.
	 * <p>
	 * This property is not synchronized automatically from the client side, so
	 * the returned value may not be the same as in client side.
	 */
	public JsonObject getSelectionEnd() {
		return (JsonObject) getElement().getPropertyRaw("selectionEnd");
	}

	/**
	 * @return It would return a boolean
	 */
	@NotSupported
	protected void hasValidator() {
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Returns true if {@code value} is valid. The validator provided in
	 * {@code validator} will be used first, if it exists; otherwise, the
	 * {@code textarea}'s validity is used.
	 * 
	 * @return It would return a boolean
	 */
	@NotSupported
	protected void validate() {
	}

	@DomEvent("invalid-changed")
	public static class InvalidChangedEvent
			extends
				ComponentEvent<GeneratedIronAutogrowTextarea> {
		public InvalidChangedEvent(GeneratedIronAutogrowTextarea source,
				boolean fromClient) {
			super(source, fromClient);
		}
	}

	public Registration addInvalidChangedListener(
			ComponentEventListener<InvalidChangedEvent> listener) {
		return addListener(InvalidChangedEvent.class, listener);
	}

	@DomEvent("focused-changed")
	public static class FocusedChangedEvent
			extends
				ComponentEvent<GeneratedIronAutogrowTextarea> {
		public FocusedChangedEvent(GeneratedIronAutogrowTextarea source,
				boolean fromClient) {
			super(source, fromClient);
		}
	}

	public Registration addFocusedChangedListener(
			ComponentEventListener<FocusedChangedEvent> listener) {
		return addListener(FocusedChangedEvent.class, listener);
	}

	@DomEvent("disabled-changed")
	public static class DisabledChangedEvent
			extends
				ComponentEvent<GeneratedIronAutogrowTextarea> {
		public DisabledChangedEvent(GeneratedIronAutogrowTextarea source,
				boolean fromClient) {
			super(source, fromClient);
		}
	}

	public Registration addDisabledChangedListener(
			ComponentEventListener<DisabledChangedEvent> listener) {
		return addListener(DisabledChangedEvent.class, listener);
	}

	@DomEvent("value-changed")
	public static class ValueChangedEvent
			extends
				ComponentEvent<GeneratedIronAutogrowTextarea> {
		public ValueChangedEvent(GeneratedIronAutogrowTextarea source,
				boolean fromClient) {
			super(source, fromClient);
		}
	}

	public Registration addValueChangedListener(
			ComponentEventListener<ValueChangedEvent> listener) {
		return addListener(ValueChangedEvent.class, listener);
	}

	@DomEvent("bind-value-changed")
	public static class BindValueChangedEvent
			extends
				ComponentEvent<GeneratedIronAutogrowTextarea> {
		public BindValueChangedEvent(GeneratedIronAutogrowTextarea source,
				boolean fromClient) {
			super(source, fromClient);
		}
	}

	public Registration addBindValueChangedListener(
			ComponentEventListener<BindValueChangedEvent> listener) {
		return addListener(BindValueChangedEvent.class, listener);
	}

	/**
	 * Gets the narrow typed reference to this object. Subclasses should
	 * override this method to support method chaining using the inherited type.
	 * 
	 * @return This object casted to its type.
	 */
	protected <R extends GeneratedIronAutogrowTextarea> R getSelf() {
		return (R) this;
	}
}