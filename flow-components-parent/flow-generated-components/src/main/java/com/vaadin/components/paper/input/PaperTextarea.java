package com.vaadin.components.paper.input;

import com.vaadin.ui.Component;
import javax.annotation.Generated;
import com.vaadin.annotations.Tag;
import elemental.json.JsonObject;
import com.vaadin.shared.Registration;
import com.vaadin.flow.dom.DomEventListener;

/**
 * Description copied from corresponding location in WebComponent:
 * 
 * `<paper-textarea>` is a multi-line text field with Material Design styling.
 * 
 * <paper-textarea label="Textarea label"></paper-textarea>
 * 
 * See `Polymer.PaperInputBehavior` for more API docs.
 * 
 * ### Validation
 * 
 * Currently only `required` and `maxlength` validation is supported.
 * 
 * ### Styling
 * 
 * See `Polymer.PaperInputContainer` for a list of custom properties used to
 * style this element.
 */
@Generated({
		"Generator: com.vaadin.generator.ComponentGenerator#0.1.9-SNAPSHOT",
		"WebComponent: paper-input/paper-textarea.html/paper-textarea#2.0.1",
		"Flow#0.1.9-SNAPSHOT"})
@Tag("paper-textarea")
public class PaperTextarea extends Component {

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
	 * Set to true to disable this input. If you're using PaperInputBehavior to
	 * implement your own paper-input-like element, bind this to both the
	 * `<paper-input-container>`'s and the input's `disabled` property.
	 */
	public boolean isDisabled() {
		return getElement().getProperty("disabled", false);
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Set to true to disable this input. If you're using PaperInputBehavior to
	 * implement your own paper-input-like element, bind this to both the
	 * `<paper-input-container>`'s and the input's `disabled` property.
	 * 
	 * @param disabled
	 */
	public void setDisabled(boolean disabled) {
		getElement().setProperty("disabled", disabled);
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * The EventTarget that will be firing relevant KeyboardEvents. Set it to
	 * `null` to disable the listeners.
	 */
	public JsonObject getKeyEventTarget() {
		return (JsonObject) getElement().getPropertyRaw("keyEventTarget");
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * The EventTarget that will be firing relevant KeyboardEvents. Set it to
	 * `null` to disable the listeners.
	 * 
	 * @param keyEventTarget
	 */
	public void setKeyEventTarget(elemental.json.JsonObject keyEventTarget) {
		getElement().setPropertyJson("keyEventTarget", keyEventTarget);
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * If true, this property will cause the implementing element to
	 * automatically stop propagation on any handled KeyboardEvents.
	 */
	public boolean isStopKeyboardEventPropagation() {
		return getElement().getProperty("stopKeyboardEventPropagation", false);
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * If true, this property will cause the implementing element to
	 * automatically stop propagation on any handled KeyboardEvents.
	 * 
	 * @param stopKeyboardEventPropagation
	 */
	public void setStopKeyboardEventPropagation(
			boolean stopKeyboardEventPropagation) {
		getElement().setProperty("stopKeyboardEventPropagation",
				stopKeyboardEventPropagation);
	}

	public JsonObject getKeyBindings() {
		return (JsonObject) getElement().getPropertyRaw("keyBindings");
	}

	/**
	 * @param keyBindings
	 */
	public void setKeyBindings(elemental.json.JsonObject keyBindings) {
		getElement().setPropertyJson("keyBindings", keyBindings);
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * The label for this input. If you're using PaperInputBehavior to implement
	 * your own paper-input-like element, bind this to `<label>`'s content and
	 * `hidden` property, e.g. `<label hidden$="[[!label]]">[[label]]</label>`
	 * in your `template`
	 */
	public String getLabel() {
		return getElement().getProperty("label");
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * The label for this input. If you're using PaperInputBehavior to implement
	 * your own paper-input-like element, bind this to `<label>`'s content and
	 * `hidden` property, e.g. `<label hidden$="[[!label]]">[[label]]</label>`
	 * in your `template`
	 * 
	 * @param label
	 */
	public void setLabel(java.lang.String label) {
		getElement().setProperty("label", label == null ? "" : label);
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * The value for this element.
	 */
	public String getValue() {
		return getElement().getProperty("value");
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * The value for this element.
	 * 
	 * @param value
	 */
	public void setValue(java.lang.String value) {
		getElement().setProperty("value", value == null ? "" : value);
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Returns true if the value is invalid. If you're using PaperInputBehavior
	 * to implement your own paper-input-like element, bind this to both the
	 * `<paper-input-container>`'s and the input's `invalid` property.
	 * 
	 * If `autoValidate` is true, the `invalid` attribute is managed
	 * automatically, which can clobber attempts to manage it manually.
	 */
	public boolean isInvalid() {
		return getElement().getProperty("invalid", false);
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Returns true if the value is invalid. If you're using PaperInputBehavior
	 * to implement your own paper-input-like element, bind this to both the
	 * `<paper-input-container>`'s and the input's `invalid` property.
	 * 
	 * If `autoValidate` is true, the `invalid` attribute is managed
	 * automatically, which can clobber attempts to manage it manually.
	 * 
	 * @param invalid
	 */
	public void setInvalid(boolean invalid) {
		getElement().setProperty("invalid", invalid);
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Set this to specify the pattern allowed by `preventInvalidInput`. If
	 * you're using PaperInputBehavior to implement your own paper-input-like
	 * element, bind this to the `<input is="iron-input">`'s `allowedPattern`
	 * property.
	 */
	public String getAllowedPattern() {
		return getElement().getProperty("allowedPattern");
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Set this to specify the pattern allowed by `preventInvalidInput`. If
	 * you're using PaperInputBehavior to implement your own paper-input-like
	 * element, bind this to the `<input is="iron-input">`'s `allowedPattern`
	 * property.
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
	 * The type of the input. The supported types are `text`, `number` and
	 * `password`. If you're using PaperInputBehavior to implement your own
	 * paper-input-like element, bind this to the `<input is="iron-input">`'s
	 * `type` property.
	 */
	public String getType() {
		return getElement().getProperty("type");
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * The type of the input. The supported types are `text`, `number` and
	 * `password`. If you're using PaperInputBehavior to implement your own
	 * paper-input-like element, bind this to the `<input is="iron-input">`'s
	 * `type` property.
	 * 
	 * @param type
	 */
	public void setType(java.lang.String type) {
		getElement().setProperty("type", type == null ? "" : type);
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * The datalist of the input (if any). This should match the id of an
	 * existing `<datalist>`. If you're using PaperInputBehavior to implement
	 * your own paper-input-like element, bind this to the `<input
	 * is="iron-input">`'s `list` property.
	 */
	public String getList() {
		return getElement().getProperty("list");
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * The datalist of the input (if any). This should match the id of an
	 * existing `<datalist>`. If you're using PaperInputBehavior to implement
	 * your own paper-input-like element, bind this to the `<input
	 * is="iron-input">`'s `list` property.
	 * 
	 * @param list
	 */
	public void setList(java.lang.String list) {
		getElement().setProperty("list", list == null ? "" : list);
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * A pattern to validate the `input` with. If you're using
	 * PaperInputBehavior to implement your own paper-input-like element, bind
	 * this to the `<input is="iron-input">`'s `pattern` property.
	 */
	public String getPattern() {
		return getElement().getProperty("pattern");
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * A pattern to validate the `input` with. If you're using
	 * PaperInputBehavior to implement your own paper-input-like element, bind
	 * this to the `<input is="iron-input">`'s `pattern` property.
	 * 
	 * @param pattern
	 */
	public void setPattern(java.lang.String pattern) {
		getElement().setProperty("pattern", pattern == null ? "" : pattern);
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Set to true to mark the input as required. If used in a form, a custom
	 * element that uses this behavior should also use
	 * Polymer.IronValidatableBehavior and define a custom validation method.
	 * Otherwise, a `required` element will always be considered valid. It's
	 * also strongly recommended to provide a visual style for the element when
	 * its value is invalid.
	 */
	public boolean isRequired() {
		return getElement().getProperty("required", false);
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Set to true to mark the input as required. If used in a form, a custom
	 * element that uses this behavior should also use
	 * Polymer.IronValidatableBehavior and define a custom validation method.
	 * Otherwise, a `required` element will always be considered valid. It's
	 * also strongly recommended to provide a visual style for the element when
	 * its value is invalid.
	 * 
	 * @param required
	 */
	public void setRequired(boolean required) {
		getElement().setProperty("required", required);
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * The error message to display when the input is invalid. If you're using
	 * PaperInputBehavior to implement your own paper-input-like element, bind
	 * this to the `<paper-input-error>`'s content, if using.
	 */
	public String getErrorMessage() {
		return getElement().getProperty("errorMessage");
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * The error message to display when the input is invalid. If you're using
	 * PaperInputBehavior to implement your own paper-input-like element, bind
	 * this to the `<paper-input-error>`'s content, if using.
	 * 
	 * @param errorMessage
	 */
	public void setErrorMessage(java.lang.String errorMessage) {
		getElement().setProperty("errorMessage",
				errorMessage == null ? "" : errorMessage);
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Set to true to show a character counter.
	 */
	public boolean isCharCounter() {
		return getElement().getProperty("charCounter", false);
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Set to true to show a character counter.
	 * 
	 * @param charCounter
	 */
	public void setCharCounter(boolean charCounter) {
		getElement().setProperty("charCounter", charCounter);
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Set to true to disable the floating label. If you're using
	 * PaperInputBehavior to implement your own paper-input-like element, bind
	 * this to the `<paper-input-container>`'s `noLabelFloat` property.
	 */
	public boolean isNoLabelFloat() {
		return getElement().getProperty("noLabelFloat", false);
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Set to true to disable the floating label. If you're using
	 * PaperInputBehavior to implement your own paper-input-like element, bind
	 * this to the `<paper-input-container>`'s `noLabelFloat` property.
	 * 
	 * @param noLabelFloat
	 */
	public void setNoLabelFloat(boolean noLabelFloat) {
		getElement().setProperty("noLabelFloat", noLabelFloat);
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Set to true to always float the label. If you're using PaperInputBehavior
	 * to implement your own paper-input-like element, bind this to the
	 * `<paper-input-container>`'s `alwaysFloatLabel` property.
	 */
	public boolean isAlwaysFloatLabel() {
		return getElement().getProperty("alwaysFloatLabel", false);
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Set to true to always float the label. If you're using PaperInputBehavior
	 * to implement your own paper-input-like element, bind this to the
	 * `<paper-input-container>`'s `alwaysFloatLabel` property.
	 * 
	 * @param alwaysFloatLabel
	 */
	public void setAlwaysFloatLabel(boolean alwaysFloatLabel) {
		getElement().setProperty("alwaysFloatLabel", alwaysFloatLabel);
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Set to true to auto-validate the input value. If you're using
	 * PaperInputBehavior to implement your own paper-input-like element, bind
	 * this to the `<paper-input-container>`'s `autoValidate` property.
	 */
	public boolean isAutoValidate() {
		return getElement().getProperty("autoValidate", false);
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Set to true to auto-validate the input value. If you're using
	 * PaperInputBehavior to implement your own paper-input-like element, bind
	 * this to the `<paper-input-container>`'s `autoValidate` property.
	 * 
	 * @param autoValidate
	 */
	public void setAutoValidate(boolean autoValidate) {
		getElement().setProperty("autoValidate", autoValidate);
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Name of the validator to use. If you're using PaperInputBehavior to
	 * implement your own paper-input-like element, bind this to the `<input
	 * is="iron-input">`'s `validator` property.
	 */
	public String getValidator() {
		return getElement().getProperty("validator");
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Name of the validator to use. If you're using PaperInputBehavior to
	 * implement your own paper-input-like element, bind this to the `<input
	 * is="iron-input">`'s `validator` property.
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
	 * If you're using PaperInputBehavior to implement your own paper-input-like
	 * element, bind this to the `<input is="iron-input">`'s `autocomplete`
	 * property.
	 */
	public String getAutocomplete() {
		return getElement().getProperty("autocomplete");
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * If you're using PaperInputBehavior to implement your own paper-input-like
	 * element, bind this to the `<input is="iron-input">`'s `autocomplete`
	 * property.
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
	 * If you're using PaperInputBehavior to implement your own paper-input-like
	 * element, bind this to the `<input is="iron-input">`'s `autofocus`
	 * property.
	 */
	public boolean isAutofocus() {
		return getElement().getProperty("autofocus", false);
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * If you're using PaperInputBehavior to implement your own paper-input-like
	 * element, bind this to the `<input is="iron-input">`'s `autofocus`
	 * property.
	 * 
	 * @param autofocus
	 */
	public void setAutofocus(boolean autofocus) {
		getElement().setProperty("autofocus", autofocus);
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * If you're using PaperInputBehavior to implement your own paper-input-like
	 * element, bind this to the `<input is="iron-input">`'s `inputmode`
	 * property.
	 */
	public String getInputmode() {
		return getElement().getProperty("inputmode");
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * If you're using PaperInputBehavior to implement your own paper-input-like
	 * element, bind this to the `<input is="iron-input">`'s `inputmode`
	 * property.
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
	 * The minimum length of the input value. If you're using PaperInputBehavior
	 * to implement your own paper-input-like element, bind this to the `<input
	 * is="iron-input">`'s `minlength` property.
	 */
	public double getMinlength() {
		return getElement().getProperty("minlength", 0.0);
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * The minimum length of the input value. If you're using PaperInputBehavior
	 * to implement your own paper-input-like element, bind this to the `<input
	 * is="iron-input">`'s `minlength` property.
	 * 
	 * @param minlength
	 */
	public void setMinlength(double minlength) {
		getElement().setProperty("minlength", minlength);
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * The maximum length of the input value. If you're using PaperInputBehavior
	 * to implement your own paper-input-like element, bind this to the `<input
	 * is="iron-input">`'s `maxlength` property.
	 */
	public double getMaxlength() {
		return getElement().getProperty("maxlength", 0.0);
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * The maximum length of the input value. If you're using PaperInputBehavior
	 * to implement your own paper-input-like element, bind this to the `<input
	 * is="iron-input">`'s `maxlength` property.
	 * 
	 * @param maxlength
	 */
	public void setMaxlength(double maxlength) {
		getElement().setProperty("maxlength", maxlength);
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * The minimum (numeric or date-time) input value. If you're using
	 * PaperInputBehavior to implement your own paper-input-like element, bind
	 * this to the `<input is="iron-input">`'s `min` property.
	 */
	public String getMin() {
		return getElement().getProperty("min");
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * The minimum (numeric or date-time) input value. If you're using
	 * PaperInputBehavior to implement your own paper-input-like element, bind
	 * this to the `<input is="iron-input">`'s `min` property.
	 * 
	 * @param min
	 */
	public void setMin(java.lang.String min) {
		getElement().setProperty("min", min == null ? "" : min);
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * The maximum (numeric or date-time) input value. Can be a String (e.g.
	 * `"2000-01-01"`) or a Number (e.g. `2`). If you're using
	 * PaperInputBehavior to implement your own paper-input-like element, bind
	 * this to the `<input is="iron-input">`'s `max` property.
	 */
	public String getMax() {
		return getElement().getProperty("max");
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * The maximum (numeric or date-time) input value. Can be a String (e.g.
	 * `"2000-01-01"`) or a Number (e.g. `2`). If you're using
	 * PaperInputBehavior to implement your own paper-input-like element, bind
	 * this to the `<input is="iron-input">`'s `max` property.
	 * 
	 * @param max
	 */
	public void setMax(java.lang.String max) {
		getElement().setProperty("max", max == null ? "" : max);
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Limits the numeric or date-time increments. If you're using
	 * PaperInputBehavior to implement your own paper-input-like element, bind
	 * this to the `<input is="iron-input">`'s `step` property.
	 */
	public String getStep() {
		return getElement().getProperty("step");
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Limits the numeric or date-time increments. If you're using
	 * PaperInputBehavior to implement your own paper-input-like element, bind
	 * this to the `<input is="iron-input">`'s `step` property.
	 * 
	 * @param step
	 */
	public void setStep(java.lang.String step) {
		getElement().setProperty("step", step == null ? "" : step);
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * The name of this element.
	 */
	public String getName() {
		return getElement().getProperty("name");
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * The name of this element.
	 * 
	 * @param name
	 */
	public void setName(java.lang.String name) {
		getElement().setProperty("name", name == null ? "" : name);
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * A placeholder string in addition to the label. If this is set, the label
	 * will always float.
	 */
	public String getPlaceholder() {
		return getElement().getProperty("placeholder");
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * A placeholder string in addition to the label. If this is set, the label
	 * will always float.
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
	 * If you're using PaperInputBehavior to implement your own paper-input-like
	 * element, bind this to the `<input is="iron-input">`'s `readonly`
	 * property.
	 */
	public boolean isReadonly() {
		return getElement().getProperty("readonly", false);
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * If you're using PaperInputBehavior to implement your own paper-input-like
	 * element, bind this to the `<input is="iron-input">`'s `readonly`
	 * property.
	 * 
	 * @param readonly
	 */
	public void setReadonly(boolean readonly) {
		getElement().setProperty("readonly", readonly);
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * If you're using PaperInputBehavior to implement your own paper-input-like
	 * element, bind this to the `<input is="iron-input">`'s `size` property.
	 */
	public double getSize() {
		return getElement().getProperty("size", 0.0);
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * If you're using PaperInputBehavior to implement your own paper-input-like
	 * element, bind this to the `<input is="iron-input">`'s `size` property.
	 * 
	 * @param size
	 */
	public void setSize(double size) {
		getElement().setProperty("size", size);
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * If you're using PaperInputBehavior to implement your own paper-input-like
	 * element, bind this to the `<input is="iron-input">`'s `autocapitalize`
	 * property.
	 */
	public String getAutocapitalize() {
		return getElement().getProperty("autocapitalize");
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * If you're using PaperInputBehavior to implement your own paper-input-like
	 * element, bind this to the `<input is="iron-input">`'s `autocapitalize`
	 * property.
	 * 
	 * @param autocapitalize
	 */
	public void setAutocapitalize(java.lang.String autocapitalize) {
		getElement().setProperty("autocapitalize",
				autocapitalize == null ? "" : autocapitalize);
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * If you're using PaperInputBehavior to implement your own paper-input-like
	 * element, bind this to the `<input is="iron-input">`'s `autocorrect`
	 * property.
	 */
	public String getAutocorrect() {
		return getElement().getProperty("autocorrect");
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * If you're using PaperInputBehavior to implement your own paper-input-like
	 * element, bind this to the `<input is="iron-input">`'s `autocorrect`
	 * property.
	 * 
	 * @param autocorrect
	 */
	public void setAutocorrect(java.lang.String autocorrect) {
		getElement().setProperty("autocorrect",
				autocorrect == null ? "" : autocorrect);
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * If you're using PaperInputBehavior to implement your own paper-input-like
	 * element, bind this to the `<input is="iron-input">`'s `autosave`
	 * property, used with type=search.
	 */
	public String getAutosave() {
		return getElement().getProperty("autosave");
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * If you're using PaperInputBehavior to implement your own paper-input-like
	 * element, bind this to the `<input is="iron-input">`'s `autosave`
	 * property, used with type=search.
	 * 
	 * @param autosave
	 */
	public void setAutosave(java.lang.String autosave) {
		getElement().setProperty("autosave", autosave == null ? "" : autosave);
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * If you're using PaperInputBehavior to implement your own paper-input-like
	 * element, bind this to the `<input is="iron-input">`'s `results` property,
	 * used with type=search.
	 */
	public double getResults() {
		return getElement().getProperty("results", 0.0);
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * If you're using PaperInputBehavior to implement your own paper-input-like
	 * element, bind this to the `<input is="iron-input">`'s `results` property,
	 * used with type=search.
	 * 
	 * @param results
	 */
	public void setResults(double results) {
		getElement().setProperty("results", results);
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * If you're using PaperInputBehavior to implement your own paper-input-like
	 * element, bind this to the `<input is="iron-input">`'s `accept` property,
	 * used with type=file.
	 */
	public String getAccept() {
		return getElement().getProperty("accept");
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * If you're using PaperInputBehavior to implement your own paper-input-like
	 * element, bind this to the `<input is="iron-input">`'s `accept` property,
	 * used with type=file.
	 * 
	 * @param accept
	 */
	public void setAccept(java.lang.String accept) {
		getElement().setProperty("accept", accept == null ? "" : accept);
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * If you're using PaperInputBehavior to implement your own paper-input-like
	 * element, bind this to the`<input is="iron-input">`'s `multiple` property,
	 * used with type=file.
	 */
	public boolean isMultiple() {
		return getElement().getProperty("multiple", false);
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * If you're using PaperInputBehavior to implement your own paper-input-like
	 * element, bind this to the`<input is="iron-input">`'s `multiple` property,
	 * used with type=file.
	 * 
	 * @param multiple
	 */
	public void setMultiple(boolean multiple) {
		getElement().setProperty("multiple", multiple);
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
	 * Can be used to imperatively add a key binding to the implementing
	 * element. This is the imperative equivalent of declaring a keybinding in
	 * the `keyBindings` prototype property.
	 * 
	 * @param eventString
	 * @param handlerName
	 */
	public void addOwnKeyBinding(elemental.json.JsonObject eventString,
			elemental.json.JsonObject handlerName) {
		getElement().callFunction("addOwnKeyBinding", eventString, handlerName);
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * When called, will remove all imperatively-added key bindings.
	 */
	public void removeOwnKeyBindings() {
		getElement().callFunction("removeOwnKeyBindings");
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Returns true if a keyboard event matches `eventString`.
	 * 
	 * @param event
	 * @param eventString
	 */
	public void keyboardEventMatchesKeys(elemental.json.JsonObject event,
			java.lang.String eventString) {
		getElement().callFunction("keyboardEventMatchesKeys", event,
				eventString);
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Returns a reference to the input element.
	 */
	public void inputElement() {
		getElement().callFunction("inputElement");
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Validates the input element and sets an error style if needed.
	 */
	public void validate() {
		getElement().callFunction("validate");
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Restores the cursor to its original position after updating the value.
	 * 
	 * @param newValue
	 */
	public void updateValueAndPreserveCaret(java.lang.String newValue) {
		getElement().callFunction("updateValueAndPreserveCaret", newValue);
	}

	public Registration addFocusedChangedListener(DomEventListener listener) {
		return getElement().addEventListener("focused-changed", listener);
	}

	public Registration addDisabledChangedListener(
			com.vaadin.flow.dom.DomEventListener listener) {
		return getElement().addEventListener("disabled-changed", listener);
	}

	public Registration addChangeListener(
			com.vaadin.flow.dom.DomEventListener listener) {
		return getElement().addEventListener("change", listener);
	}

	public Registration addValueChangedListener(
			com.vaadin.flow.dom.DomEventListener listener) {
		return getElement().addEventListener("value-changed", listener);
	}

	public Registration addInvalidChangedListener(
			com.vaadin.flow.dom.DomEventListener listener) {
		return getElement().addEventListener("invalid-changed", listener);
	}

	public Registration addIronFormElementRegisterListener(
			com.vaadin.flow.dom.DomEventListener listener) {
		return getElement().addEventListener("iron-form-element-register",
				listener);
	}

	public Registration addIronFormElementUnregisterListener(
			com.vaadin.flow.dom.DomEventListener listener) {
		return getElement().addEventListener("iron-form-element-unregister",
				listener);
	}
}