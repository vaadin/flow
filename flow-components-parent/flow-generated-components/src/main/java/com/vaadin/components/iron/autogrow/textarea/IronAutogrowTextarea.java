package com.vaadin.components.iron.autogrow.textarea;

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
		"Generator: com.vaadin.generator.ComponentGenerator#0.1.10-SNAPSHOT",
		"WebComponent: iron-autogrow-textarea#2.0.0", "Flow#0.1.10-SNAPSHOT"})
@Tag("iron-autogrow-textarea")
@HtmlImport("frontend://bower_components/iron-autogrow-textarea/iron-autogrow-textarea.html")
public class IronAutogrowTextarea extends Component {

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
	 * True if the last call to {@code validate} is invalid.
	 */
	public boolean isInvalid() {
		return getElement().getProperty("invalid", false);
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * True if the last call to {@code validate} is invalid.
	 * 
	 * @param invalid
	 */
	public void setInvalid(boolean invalid) {
		getElement().setProperty("invalid", invalid);
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
	 */
	public void setFocused(boolean focused) {
		getElement().setProperty("focused", focused);
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
	 */
	public void setDisabled(boolean disabled) {
		getElement().setProperty("disabled", disabled);
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Use this property instead of {@code bind-value} for two-way data binding.
	 */
	public String getValueString() {
		return getElement().getProperty("value");
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Use this property instead of {@code bind-value} for two-way data binding.
	 */
	public double getValueNumber() {
		return getElement().getProperty("value", 0.0);
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Use this property instead of {@code bind-value} for two-way data binding.
	 * 
	 * @param value
	 */
	public void setValue(java.lang.String value) {
		getElement().setProperty("value", value == null ? "" : value);
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Use this property instead of {@code bind-value} for two-way data binding.
	 * 
	 * @param value
	 */
	public void setValue(double value) {
		getElement().setProperty("value", value);
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * This property is deprecated, and just mirrors {@code value}. Use
	 * {@code value} instead.
	 */
	public String getBindValueString() {
		return getElement().getProperty("bindValue");
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * This property is deprecated, and just mirrors {@code value}. Use
	 * {@code value} instead.
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
	 */
	public void setBindValue(java.lang.String bindValue) {
		getElement().setProperty("bindValue",
				bindValue == null ? "" : bindValue);
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * This property is deprecated, and just mirrors {@code value}. Use
	 * {@code value} instead.
	 * 
	 * @param bindValue
	 */
	public void setBindValue(double bindValue) {
		getElement().setProperty("bindValue", bindValue);
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * The initial number of rows.
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
	 */
	public void setRows(double rows) {
		getElement().setProperty("rows", rows);
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * The maximum number of rows this element can grow to until it scrolls. 0
	 * means no maximum.
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
	 */
	public void setMaxRows(double maxRows) {
		getElement().setProperty("maxRows", maxRows);
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Bound to the textarea's {@code autocomplete} attribute.
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
	 */
	public void setAutocomplete(java.lang.String autocomplete) {
		getElement().setProperty("autocomplete",
				autocomplete == null ? "" : autocomplete);
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Bound to the textarea's {@code autofocus} attribute.
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
	 */
	public void setAutofocus(boolean autofocus) {
		getElement().setProperty("autofocus", autofocus);
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Bound to the textarea's {@code inputmode} attribute.
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
	 */
	public void setInputmode(java.lang.String inputmode) {
		getElement().setProperty("inputmode",
				inputmode == null ? "" : inputmode);
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Bound to the textarea's {@code placeholder} attribute.
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
	 */
	public void setPlaceholder(java.lang.String placeholder) {
		getElement().setProperty("placeholder",
				placeholder == null ? "" : placeholder);
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Bound to the textarea's {@code readonly} attribute.
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
	 */
	public void setReadonly(java.lang.String readonly) {
		getElement().setProperty("readonly", readonly == null ? "" : readonly);
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Set to true to mark the textarea as required.
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
	 */
	public void setRequired(boolean required) {
		getElement().setProperty("required", required);
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * The minimum length of the input value.
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
	 */
	public void setMinlength(double minlength) {
		getElement().setProperty("minlength", minlength);
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * The maximum length of the input value.
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
	 */
	public void setMaxlength(double maxlength) {
		getElement().setProperty("maxlength", maxlength);
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Returns the underlying textarea.
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
	 */
	public void setTextarea(elemental.json.JsonObject textarea) {
		getElement().setPropertyJson("textarea", textarea);
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Returns textarea's selection start.
	 */
	public JsonObject getSelectionStart() {
		return (JsonObject) getElement().getPropertyRaw("selectionStart");
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Returns textarea's selection start.
	 * 
	 * @param selectionStart
	 */
	public void setSelectionStart(elemental.json.JsonObject selectionStart) {
		getElement().setPropertyJson("selectionStart", selectionStart);
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Returns textarea's selection end.
	 */
	public JsonObject getSelectionEnd() {
		return (JsonObject) getElement().getPropertyRaw("selectionEnd");
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Returns textarea's selection end.
	 * 
	 * @param selectionEnd
	 */
	public void setSelectionEnd(elemental.json.JsonObject selectionEnd) {
		getElement().setPropertyJson("selectionEnd", selectionEnd);
	}

	public void hasValidator() {
		getElement().callFunction("hasValidator");
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Returns true if {@code value} is valid. The validator provided in
	 * {@code validator} will be used first, if it exists; otherwise, the
	 * {@code textarea}'s validity is used.
	 */
	public void validate() {
		getElement().callFunction("validate");
	}

	public Registration addInvalidChangedListener(DomEventListener listener) {
		return getElement().addEventListener("invalid-changed", listener);
	}

	public Registration addFocusedChangedListener(
			com.vaadin.flow.dom.DomEventListener listener) {
		return getElement().addEventListener("focused-changed", listener);
	}

	public Registration addDisabledChangedListener(
			com.vaadin.flow.dom.DomEventListener listener) {
		return getElement().addEventListener("disabled-changed", listener);
	}

	public Registration addValueChangedListener(
			com.vaadin.flow.dom.DomEventListener listener) {
		return getElement().addEventListener("value-changed", listener);
	}

	public Registration addBindValueChangedListener(
			com.vaadin.flow.dom.DomEventListener listener) {
		return getElement().addEventListener("bind-value-changed", listener);
	}
}