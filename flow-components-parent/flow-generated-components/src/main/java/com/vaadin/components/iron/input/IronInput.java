package com.vaadin.components.iron.input;

import com.vaadin.ui.Component;
import javax.annotation.Generated;
import com.vaadin.annotations.Tag;
import com.vaadin.annotations.HtmlImport;
import elemental.json.JsonObject;
import com.vaadin.shared.Registration;
import com.vaadin.flow.dom.DomEventListener;

/**
 * Description copied from corresponding location in WebComponent:
 * 
 * `<iron-input>` is a wrapper to a native `<input>` element, that adds two-way
 * binding and prevention of invalid input. To use it, you must distribute a
 * native `<input>` yourself. You can continue to use the native `input` as you
 * would normally:
 * 
 * <iron-input> <input> </iron-input>
 * 
 * <iron-input> <input type="email" disabled> </iron-input>
 * 
 * ### Two-way binding
 * 
 * By default you can only get notified of changes to a native `<input>`'s
 * `value` due to user input:
 * 
 * <input value="{{myValue::input}}">
 * 
 * This means that if you imperatively set the value (i.e.
 * `someNativeInput.value = 'foo'`), no events will be fired and this change
 * cannot be observed.
 * 
 * `iron-input` adds the `bind-value` property that mirrors the native `input`'s
 * '`value` property; this property can be used for two-way data binding.
 * `bind-value` will notify if it is changed either by user input or by script.
 * 
 * <iron-input bind-value="{{myValue}}"> <input> </iron-input>
 * 
 * Note: this means that if you want to imperatively set the native `input`'s,
 * you _must_ set `bind-value` instead, so that the wrapper `iron-input` can be
 * notified.
 * 
 * ### Validation
 * 
 * `iron-input` uses the native `input`'s validation. For simplicity,
 * `iron-input` has a `validate()` method (which internally just checks the
 * distributed `input`'s validity), which sets an `invalid` attribute that can
 * also be used for styling.
 * 
 * To validate automatically as you type, you can use the `auto-validate`
 * attribute.
 * 
 * `iron-input` also fires an `iron-input-validate` event after `validate()` is
 * called. You can use it to implement a custom validator:
 * 
 * var CatsOnlyValidator = { validate: function(ironInput) { var valid =
 * !ironInput.bindValue || ironInput.bindValue === 'cat'; ironInput.invalid =
 * !valid; return valid; } } ironInput.addEventListener('iron-input-validate',
 * function() { CatsOnly.validate(input2); });
 * 
 * You can also use an element implementing an
 * [`IronValidatorBehavior`](/element
 * /PolymerElements/iron-validatable-behavior). This example can also be found
 * in the demo for this element:
 * 
 * <iron-input validator="cats-only"> <input> </iron-input>
 * 
 * ### Preventing invalid input
 * 
 * It may be desirable to only allow users to enter certain characters. You can
 * use the `allowed-pattern` attribute to accomplish this. This feature is
 * separate from validation, and `allowed-pattern` does not affect how the input
 * is validated.
 * 
 * // Only allow typing digits, but a valid input has exactly 5 digits.
 * <iron-input allowed-pattern="[0-9]"> <input pattern="\d{5}"> </iron-input>
 */
@Generated({
		"Generator: com.vaadin.generator.ComponentGenerator#0.1.9-SNAPSHOT",
		"WebComponent: iron-input#2.0.0", "Flow#0.1.9-SNAPSHOT"})
@Tag("iron-input")
@HtmlImport("frontend://bower_components/iron-input/iron-input.html")
public class IronInput extends Component {

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Name of the validator to use.
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
	 */
	public void setValidator(java.lang.String validator) {
		getElement().setProperty("validator",
				validator == null ? "" : validator);
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * True if the last call to `validate` is invalid.
	 */
	public boolean isInvalid() {
		return getElement().getProperty("invalid", false);
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * True if the last call to `validate` is invalid.
	 * 
	 * @param invalid
	 */
	public void setInvalid(boolean invalid) {
		getElement().setProperty("invalid", invalid);
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Use this property instead of `value` for two-way data binding, or to set
	 * a default value for the input. **Do not** use the distributed input's
	 * `value` property to set a default value.
	 */
	public String getBindValue() {
		return getElement().getProperty("bindValue");
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Use this property instead of `value` for two-way data binding, or to set
	 * a default value for the input. **Do not** use the distributed input's
	 * `value` property to set a default value.
	 * 
	 * @param bindValue
	 */
	public void setBindValue(java.lang.String bindValue) {
		getElement().setProperty("bindValue",
				bindValue == null ? "" : bindValue);
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Computed property that echoes `bindValue` (mostly used for Polymer 1.0
	 * backcompatibility, if you were one-way binding to the Polymer 1.0 `input
	 * is="iron-input"` value attribute).
	 */
	public JsonObject getValue() {
		return (JsonObject) getElement().getPropertyRaw("value");
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Computed property that echoes `bindValue` (mostly used for Polymer 1.0
	 * backcompatibility, if you were one-way binding to the Polymer 1.0 `input
	 * is="iron-input"` value attribute).
	 * 
	 * @param value
	 */
	public void setValue(elemental.json.JsonObject value) {
		getElement().setPropertyJson("value", value);
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Regex-like list of characters allowed as input; all characters not in the
	 * list will be rejected. The recommended format should be a list of allowed
	 * characters, for example, `[a-zA-Z0-9.+-!;:]`.
	 * 
	 * This pattern represents the allowed characters for the field; as the user
	 * inputs text, each individual character will be checked against the
	 * pattern (rather than checking the entire value as a whole). If a
	 * character is not a match, it will be rejected.
	 * 
	 * Pasted input will have each character checked individually; if any
	 * character doesn't match `allowedPattern`, the entire pasted string will
	 * be rejected.
	 * 
	 * Note: if you were using `iron-input` in 1.0, you were also required to
	 * set `prevent-invalid-input`. This is no longer needed as of Polymer 2.0,
	 * and will be set automatically for you if an `allowedPattern` is provided.
	 */
	public String getAllowedPattern() {
		return getElement().getProperty("allowedPattern");
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Regex-like list of characters allowed as input; all characters not in the
	 * list will be rejected. The recommended format should be a list of allowed
	 * characters, for example, `[a-zA-Z0-9.+-!;:]`.
	 * 
	 * This pattern represents the allowed characters for the field; as the user
	 * inputs text, each individual character will be checked against the
	 * pattern (rather than checking the entire value as a whole). If a
	 * character is not a match, it will be rejected.
	 * 
	 * Pasted input will have each character checked individually; if any
	 * character doesn't match `allowedPattern`, the entire pasted string will
	 * be rejected.
	 * 
	 * Note: if you were using `iron-input` in 1.0, you were also required to
	 * set `prevent-invalid-input`. This is no longer needed as of Polymer 2.0,
	 * and will be set automatically for you if an `allowedPattern` is provided.
	 * 
	 * 
	 * @param allowedPattern
	 */
	public void setAllowedPattern(java.lang.String allowedPattern) {
		getElement().setProperty("allowedPattern",
				allowedPattern == null ? "" : allowedPattern);
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Set to true to auto-validate the input value as you type.
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
	 */
	public void setAutoValidate(boolean autoValidate) {
		getElement().setProperty("autoValidate", autoValidate);
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Returns the distributed <input> element.
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
	 */
	public void setInputElement(elemental.json.JsonObject inputElement) {
		getElement().setPropertyJson("inputElement", inputElement);
	}

	public void hasValidator() {
		getElement().callFunction("hasValidator");
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Returns true if `value` is valid. The validator provided in `validator`
	 * will be used first, then any constraints.
	 */
	public void validate() {
		getElement().callFunction("validate");
	}

	public Registration addInvalidChangedListener(DomEventListener listener) {
		return getElement().addEventListener("invalid-changed", listener);
	}

	public Registration addIronInputValidateListener(
			com.vaadin.flow.dom.DomEventListener listener) {
		return getElement().addEventListener("iron-input-validate", listener);
	}
}