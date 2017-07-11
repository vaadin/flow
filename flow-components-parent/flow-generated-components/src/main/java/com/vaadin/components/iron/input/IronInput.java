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
package com.vaadin.components.iron.input;

import com.vaadin.ui.Component;
import com.vaadin.ui.HasStyle;
import javax.annotation.Generated;
import com.vaadin.annotations.Tag;
import com.vaadin.annotations.HtmlImport;
import com.vaadin.components.iron.input.IronInput;
import com.vaadin.annotations.Synchronize;
import elemental.json.JsonObject;
import com.vaadin.components.NotSupported;
import com.vaadin.annotations.DomEvent;
import com.vaadin.ui.ComponentEvent;
import com.vaadin.flow.event.ComponentEventListener;
import com.vaadin.shared.Registration;
import com.vaadin.ui.HasComponents;

/**
 * Description copied from corresponding location in WebComponent:
 * 
 * {@code <iron-input>} is a wrapper to a native {@code <input>} element, that
 * adds two-way binding and prevention of invalid input. To use it, you must
 * distribute a native {@code <input>} yourself. You can continue to use the
 * native {@code input} as you would normally:
 * 
 * <iron-input> <input> </iron-input>
 * 
 * <iron-input> <input type="email" disabled> </iron-input>
 * 
 * ### Two-way binding
 * 
 * By default you can only get notified of changes to a native {@code <input>}'s
 * {@code value} due to user input:
 * 
 * <input value="{{myValue::input}}">
 * 
 * This means that if you imperatively set the value (i.e.
 * {@code someNativeInput.value = 'foo'}), no events will be fired and this
 * change cannot be observed.
 * 
 * {@code iron-input} adds the {@code bind-value} property that mirrors the
 * native {@code input}'s '{@code value} property; this property can be used for
 * two-way data binding. {@code bind-value} will notify if it is changed either
 * by user input or by script.
 * 
 * <iron-input bind-value="{{myValue}}"> <input> </iron-input>
 * 
 * Note: this means that if you want to imperatively set the native
 * {@code input}'s, you _must_ set {@code bind-value} instead, so that the
 * wrapper {@code iron-input} can be notified.
 * 
 * ### Validation
 * 
 * {@code iron-input} uses the native {@code input}'s validation. For
 * simplicity, {@code iron-input} has a {@code validate()} method (which
 * internally just checks the distributed {@code input}'s validity), which sets
 * an {@code invalid} attribute that can also be used for styling.
 * 
 * To validate automatically as you type, you can use the {@code auto-validate}
 * attribute.
 * 
 * {@code iron-input} also fires an {@code iron-input-validate} event after
 * {@code validate()} is called. You can use it to implement a custom validator:
 * 
 * var CatsOnlyValidator = { validate: function(ironInput) { var valid =
 * !ironInput.bindValue || ironInput.bindValue === 'cat'; ironInput.invalid =
 * !valid; return valid; } } ironInput.addEventListener('iron-input-validate',
 * function() { CatsOnly.validate(input2); });
 * 
 * You can also use an element implementing an [{@code IronValidatorBehavior}
 * ](/element/PolymerElements/iron-validatable-behavior). This example can also
 * be found in the demo for this element:
 * 
 * <iron-input validator="cats-only"> <input> </iron-input>
 * 
 * ### Preventing invalid input
 * 
 * It may be desirable to only allow users to enter certain characters. You can
 * use the {@code allowed-pattern} attribute to accomplish this. This feature is
 * separate from validation, and {@code allowed-pattern} does not affect how the
 * input is validated.
 * 
 * // Only allow typing digits, but a valid input has exactly 5 digits.
 * <iron-input allowed-pattern="[0-9]"> <input pattern="\d{5}"> </iron-input>
 */
@Generated({
		"Generator: com.vaadin.generator.ComponentGenerator#0.1.13-SNAPSHOT",
		"WebComponent: iron-input#2.0.0", "Flow#0.1.13-SNAPSHOT"})
@Tag("iron-input")
@HtmlImport("frontend://bower_components/iron-input/iron-input.html")
public class IronInput extends Component implements HasStyle, HasComponents {

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
	 * @return this instance, for method chaining
	 */
	public <R extends IronInput> R setValidator(java.lang.String validator) {
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
	 * @return this instance, for method chaining
	 */
	public <R extends IronInput> R setInvalid(boolean invalid) {
		getElement().setProperty("invalid", invalid);
		return getSelf();
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Use this property instead of {@code value} for two-way data binding, or
	 * to set a default value for the input. **Do not** use the distributed
	 * input's {@code value} property to set a default value.
	 * <p>
	 * This property is not synchronized automatically from the client side, so
	 * the returned value may not be the same as in client side.
	 */
	public String getBindValue() {
		return getElement().getProperty("bindValue");
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Use this property instead of {@code value} for two-way data binding, or
	 * to set a default value for the input. **Do not** use the distributed
	 * input's {@code value} property to set a default value.
	 * 
	 * @param bindValue
	 * @return this instance, for method chaining
	 */
	public <R extends IronInput> R setBindValue(java.lang.String bindValue) {
		getElement().setProperty("bindValue",
				bindValue == null ? "" : bindValue);
		return getSelf();
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Computed property that echoes {@code bindValue} (mostly used for Polymer
	 * 1.0 backcompatibility, if you were one-way binding to the Polymer 1.0
	 * {@code input is="iron-input"} value attribute).
	 * <p>
	 * This property is not synchronized automatically from the client side, so
	 * the returned value may not be the same as in client side.
	 */
	public JsonObject getValue() {
		return (JsonObject) getElement().getPropertyRaw("value");
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Computed property that echoes {@code bindValue} (mostly used for Polymer
	 * 1.0 backcompatibility, if you were one-way binding to the Polymer 1.0
	 * {@code input is="iron-input"} value attribute).
	 * 
	 * @param value
	 * @return this instance, for method chaining
	 */
	public <R extends IronInput> R setValue(elemental.json.JsonObject value) {
		getElement().setPropertyJson("value", value);
		return getSelf();
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Regex-like list of characters allowed as input; all characters not in the
	 * list will be rejected. The recommended format should be a list of allowed
	 * characters, for example, {@code [a-zA-Z0-9.+-!;:]}.
	 * 
	 * This pattern represents the allowed characters for the field; as the user
	 * inputs text, each individual character will be checked against the
	 * pattern (rather than checking the entire value as a whole). If a
	 * character is not a match, it will be rejected.
	 * 
	 * Pasted input will have each character checked individually; if any
	 * character doesn't match {@code allowedPattern}, the entire pasted string
	 * will be rejected.
	 * 
	 * Note: if you were using {@code iron-input} in 1.0, you were also required
	 * to set {@code prevent-invalid-input}. This is no longer needed as of
	 * Polymer 2.0, and will be set automatically for you if an
	 * {@code allowedPattern} is provided.
	 * 
	 * <p>
	 * This property is not synchronized automatically from the client side, so
	 * the returned value may not be the same as in client side.
	 */
	public String getAllowedPattern() {
		return getElement().getProperty("allowedPattern");
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Regex-like list of characters allowed as input; all characters not in the
	 * list will be rejected. The recommended format should be a list of allowed
	 * characters, for example, {@code [a-zA-Z0-9.+-!;:]}.
	 * 
	 * This pattern represents the allowed characters for the field; as the user
	 * inputs text, each individual character will be checked against the
	 * pattern (rather than checking the entire value as a whole). If a
	 * character is not a match, it will be rejected.
	 * 
	 * Pasted input will have each character checked individually; if any
	 * character doesn't match {@code allowedPattern}, the entire pasted string
	 * will be rejected.
	 * 
	 * Note: if you were using {@code iron-input} in 1.0, you were also required
	 * to set {@code prevent-invalid-input}. This is no longer needed as of
	 * Polymer 2.0, and will be set automatically for you if an
	 * {@code allowedPattern} is provided.
	 * 
	 * 
	 * @param allowedPattern
	 * @return this instance, for method chaining
	 */
	public <R extends IronInput> R setAllowedPattern(
			java.lang.String allowedPattern) {
		getElement().setProperty("allowedPattern",
				allowedPattern == null ? "" : allowedPattern);
		return getSelf();
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Set to true to auto-validate the input value as you type.
	 * <p>
	 * This property is not synchronized automatically from the client side, so
	 * the returned value may not be the same as in client side.
	 */
	public boolean isAutoValidate() {
		return getElement().getProperty("autoValidate", false);
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Set to true to auto-validate the input value as you type.
	 * 
	 * @param autoValidate
	 * @return this instance, for method chaining
	 */
	public <R extends IronInput> R setAutoValidate(boolean autoValidate) {
		getElement().setProperty("autoValidate", autoValidate);
		return getSelf();
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Returns the distributed <input> element.
	 * <p>
	 * This property is not synchronized automatically from the client side, so
	 * the returned value may not be the same as in client side.
	 */
	public JsonObject getInputElement() {
		return (JsonObject) getElement().getPropertyRaw("inputElement");
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Returns the distributed <input> element.
	 * 
	 * @param inputElement
	 * @return this instance, for method chaining
	 */
	public <R extends IronInput> R setInputElement(
			elemental.json.JsonObject inputElement) {
		getElement().setPropertyJson("inputElement", inputElement);
		return getSelf();
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
	 * {@code validator} will be used first, then any constraints.
	 * 
	 * @return It would return a boolean
	 */
	@NotSupported
	protected void validate() {
	}

	@DomEvent("invalid-changed")
	public static class InvalidChangedEvent extends ComponentEvent<IronInput> {
		public InvalidChangedEvent(IronInput source, boolean fromClient) {
			super(source, fromClient);
		}
	}

	public Registration addInvalidChangedListener(
			ComponentEventListener<InvalidChangedEvent> listener) {
		return addListener(InvalidChangedEvent.class, listener);
	}

	@DomEvent("iron-input-validate")
	public static class IronInputValidateEvent
			extends
				ComponentEvent<IronInput> {
		public IronInputValidateEvent(IronInput source, boolean fromClient) {
			super(source, fromClient);
		}
	}

	public Registration addIronInputValidateListener(
			ComponentEventListener<IronInputValidateEvent> listener) {
		return addListener(IronInputValidateEvent.class, listener);
	}

	/**
	 * Gets the narrow typed reference to this object. Subclasses should
	 * override this method to support method chaining using the inherited type.
	 * 
	 * @return This object casted to its type.
	 */
	protected <R extends IronInput> R getSelf() {
		return (R) this;
	}
}