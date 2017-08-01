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
package com.vaadin.generated.paper.input;

import com.vaadin.ui.Component;
import com.vaadin.ui.ComponentSupplier;
import com.vaadin.ui.HasStyle;
import javax.annotation.Generated;
import com.vaadin.annotations.Tag;
import com.vaadin.annotations.HtmlImport;
import com.vaadin.annotations.Synchronize;
import elemental.json.JsonObject;
import com.vaadin.components.data.HasValue;
import com.vaadin.components.NotSupported;
import com.vaadin.annotations.DomEvent;
import com.vaadin.ui.ComponentEvent;
import com.vaadin.flow.event.ComponentEventListener;
import com.vaadin.shared.Registration;

/**
 * Description copied from corresponding location in WebComponent:
 * 
 * This is a fresh new hell to make this element hybrid. Basically, in 2.0 we
 * lost is=, so the example same template can't be used with iron-input 1.0 and
 * 2.0. Expect some conditional code (especially in the tests).
 */
@Generated({
		"Generator: com.vaadin.generator.ComponentGenerator#0.1.16-SNAPSHOT",
		"WebComponent: paper-input#2.0.1", "Flow#0.1.16-SNAPSHOT"})
@Tag("paper-input")
@HtmlImport("frontend://bower_components/paper-input/paper-input.html")
public class GeneratedPaperInput<R extends GeneratedPaperInput<R>>
		extends
			Component
		implements
			ComponentSupplier<R>,
			HasStyle,
			HasValue<R, String> {

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
	 * Set to true to disable this input. If you're using PaperInputBehavior to
	 * implement your own paper-input-like element, bind this to both the
	 * {@code <paper-input-container>}'s and the input's {@code disabled}
	 * property.
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
	 * Set to true to disable this input. If you're using PaperInputBehavior to
	 * implement your own paper-input-like element, bind this to both the
	 * {@code <paper-input-container>}'s and the input's {@code disabled}
	 * property.
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
	 * The EventTarget that will be firing relevant KeyboardEvents. Set it to
	 * {@code null} to disable the listeners.
	 * <p>
	 * This property is not synchronized automatically from the client side, so
	 * the returned value may not be the same as in client side.
	 */
	protected JsonObject protectedGetKeyEventTarget() {
		return (JsonObject) getElement().getPropertyRaw("keyEventTarget");
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * The EventTarget that will be firing relevant KeyboardEvents. Set it to
	 * {@code null} to disable the listeners.
	 * 
	 * @param keyEventTarget
	 *            the JsonObject value to set
	 * @return this instance, for method chaining
	 */
	protected R setKeyEventTarget(elemental.json.JsonObject keyEventTarget) {
		getElement().setPropertyJson("keyEventTarget", keyEventTarget);
		return get();
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * If true, this property will cause the implementing element to
	 * automatically stop propagation on any handled KeyboardEvents.
	 * <p>
	 * This property is not synchronized automatically from the client side, so
	 * the returned value may not be the same as in client side.
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
	 *            the boolean value to set
	 * @return this instance, for method chaining
	 */
	public R setStopKeyboardEventPropagation(
			boolean stopKeyboardEventPropagation) {
		getElement().setProperty("stopKeyboardEventPropagation",
				stopKeyboardEventPropagation);
		return get();
	}

	/**
	 * This property is not synchronized automatically from the client side, so
	 * the returned value may not be the same as in client side.
	 */
	protected JsonObject protectedGetKeyBindings() {
		return (JsonObject) getElement().getPropertyRaw("keyBindings");
	}

	/**
	 * @param keyBindings
	 *            the JsonObject value to set
	 * @return this instance, for method chaining
	 */
	protected R setKeyBindings(elemental.json.JsonObject keyBindings) {
		getElement().setPropertyJson("keyBindings", keyBindings);
		return get();
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * The label for this input. If you're using PaperInputBehavior to implement
	 * your own paper-input-like element, bind this to {@code <label>}'s content
	 * and {@code hidden} property, e.g.
	 * {@code <label hidden$="[[!label]]">[[label]]</label>} in your
	 * {@code template}
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
	 * The label for this input. If you're using PaperInputBehavior to implement
	 * your own paper-input-like element, bind this to {@code <label>}'s content
	 * and {@code hidden} property, e.g.
	 * {@code <label hidden$="[[!label]]">[[label]]</label>} in your
	 * {@code template}
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
	 * The value for this element.
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
	 * The value for this element.
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
	 * Returns true if the value is invalid. If you're using PaperInputBehavior
	 * to implement your own paper-input-like element, bind this to both the
	 * {@code <paper-input-container>}'s and the input's {@code invalid}
	 * property.
	 * 
	 * If {@code autoValidate} is true, the {@code invalid} attribute is managed
	 * automatically, which can clobber attempts to manage it manually.
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
	 * Returns true if the value is invalid. If you're using PaperInputBehavior
	 * to implement your own paper-input-like element, bind this to both the
	 * {@code <paper-input-container>}'s and the input's {@code invalid}
	 * property.
	 * 
	 * If {@code autoValidate} is true, the {@code invalid} attribute is managed
	 * automatically, which can clobber attempts to manage it manually.
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
	 * Set this to specify the pattern allowed by {@code preventInvalidInput}.
	 * If you're using PaperInputBehavior to implement your own paper-input-like
	 * element, bind this to the {@code <input is="iron-input">}'s
	 * {@code allowedPattern} property.
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
	 * Set this to specify the pattern allowed by {@code preventInvalidInput}.
	 * If you're using PaperInputBehavior to implement your own paper-input-like
	 * element, bind this to the {@code <input is="iron-input">}'s
	 * {@code allowedPattern} property.
	 * 
	 * @param allowedPattern
	 *            the String value to set
	 * @return this instance, for method chaining
	 */
	public R setAllowedPattern(java.lang.String allowedPattern) {
		getElement().setProperty("allowedPattern",
				allowedPattern == null ? "" : allowedPattern);
		return get();
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * The type of the input. The supported types are {@code text},
	 * {@code number} and {@code password}. If you're using PaperInputBehavior
	 * to implement your own paper-input-like element, bind this to the
	 * {@code <input is="iron-input">}'s {@code type} property.
	 * <p>
	 * This property is not synchronized automatically from the client side, so
	 * the returned value may not be the same as in client side.
	 */
	public String getType() {
		return getElement().getProperty("type");
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * The type of the input. The supported types are {@code text},
	 * {@code number} and {@code password}. If you're using PaperInputBehavior
	 * to implement your own paper-input-like element, bind this to the
	 * {@code <input is="iron-input">}'s {@code type} property.
	 * 
	 * @param type
	 *            the String value to set
	 * @return this instance, for method chaining
	 */
	public R setType(java.lang.String type) {
		getElement().setProperty("type", type == null ? "" : type);
		return get();
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * The datalist of the input (if any). This should match the id of an
	 * existing {@code <datalist>}. If you're using PaperInputBehavior to
	 * implement your own paper-input-like element, bind this to the
	 * {@code <input is="iron-input">}'s {@code list} property.
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
	 * The datalist of the input (if any). This should match the id of an
	 * existing {@code <datalist>}. If you're using PaperInputBehavior to
	 * implement your own paper-input-like element, bind this to the
	 * {@code <input is="iron-input">}'s {@code list} property.
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
	 * A pattern to validate the {@code input} with. If you're using
	 * PaperInputBehavior to implement your own paper-input-like element, bind
	 * this to the {@code <input is="iron-input">}'s {@code pattern} property.
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
	 * A pattern to validate the {@code input} with. If you're using
	 * PaperInputBehavior to implement your own paper-input-like element, bind
	 * this to the {@code <input is="iron-input">}'s {@code pattern} property.
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
	 * Set to true to mark the input as required. If used in a form, a custom
	 * element that uses this behavior should also use
	 * Polymer.IronValidatableBehavior and define a custom validation method.
	 * Otherwise, a {@code required} element will always be considered valid.
	 * It's also strongly recommended to provide a visual style for the element
	 * when its value is invalid.
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
	 * Set to true to mark the input as required. If used in a form, a custom
	 * element that uses this behavior should also use
	 * Polymer.IronValidatableBehavior and define a custom validation method.
	 * Otherwise, a {@code required} element will always be considered valid.
	 * It's also strongly recommended to provide a visual style for the element
	 * when its value is invalid.
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
	 * The error message to display when the input is invalid. If you're using
	 * PaperInputBehavior to implement your own paper-input-like element, bind
	 * this to the {@code <paper-input-error>}'s content, if using.
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
	 * The error message to display when the input is invalid. If you're using
	 * PaperInputBehavior to implement your own paper-input-like element, bind
	 * this to the {@code <paper-input-error>}'s content, if using.
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
	 * Set to true to show a character counter.
	 * <p>
	 * This property is not synchronized automatically from the client side, so
	 * the returned value may not be the same as in client side.
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
	 *            the boolean value to set
	 * @return this instance, for method chaining
	 */
	public R setCharCounter(boolean charCounter) {
		getElement().setProperty("charCounter", charCounter);
		return get();
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Set to true to disable the floating label. If you're using
	 * PaperInputBehavior to implement your own paper-input-like element, bind
	 * this to the {@code <paper-input-container>}'s {@code noLabelFloat}
	 * property.
	 * <p>
	 * This property is not synchronized automatically from the client side, so
	 * the returned value may not be the same as in client side.
	 */
	public boolean isNoLabelFloat() {
		return getElement().getProperty("noLabelFloat", false);
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Set to true to disable the floating label. If you're using
	 * PaperInputBehavior to implement your own paper-input-like element, bind
	 * this to the {@code <paper-input-container>}'s {@code noLabelFloat}
	 * property.
	 * 
	 * @param noLabelFloat
	 *            the boolean value to set
	 * @return this instance, for method chaining
	 */
	public R setNoLabelFloat(boolean noLabelFloat) {
		getElement().setProperty("noLabelFloat", noLabelFloat);
		return get();
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Set to true to always float the label. If you're using PaperInputBehavior
	 * to implement your own paper-input-like element, bind this to the
	 * {@code <paper-input-container>}'s {@code alwaysFloatLabel} property.
	 * <p>
	 * This property is not synchronized automatically from the client side, so
	 * the returned value may not be the same as in client side.
	 */
	public boolean isAlwaysFloatLabel() {
		return getElement().getProperty("alwaysFloatLabel", false);
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Set to true to always float the label. If you're using PaperInputBehavior
	 * to implement your own paper-input-like element, bind this to the
	 * {@code <paper-input-container>}'s {@code alwaysFloatLabel} property.
	 * 
	 * @param alwaysFloatLabel
	 *            the boolean value to set
	 * @return this instance, for method chaining
	 */
	public R setAlwaysFloatLabel(boolean alwaysFloatLabel) {
		getElement().setProperty("alwaysFloatLabel", alwaysFloatLabel);
		return get();
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Set to true to auto-validate the input value. If you're using
	 * PaperInputBehavior to implement your own paper-input-like element, bind
	 * this to the {@code <paper-input-container>}'s {@code autoValidate}
	 * property.
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
	 * Set to true to auto-validate the input value. If you're using
	 * PaperInputBehavior to implement your own paper-input-like element, bind
	 * this to the {@code <paper-input-container>}'s {@code autoValidate}
	 * property.
	 * 
	 * @param autoValidate
	 *            the boolean value to set
	 * @return this instance, for method chaining
	 */
	public R setAutoValidate(boolean autoValidate) {
		getElement().setProperty("autoValidate", autoValidate);
		return get();
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Name of the validator to use. If you're using PaperInputBehavior to
	 * implement your own paper-input-like element, bind this to the
	 * {@code <input is="iron-input">}'s {@code validator} property.
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
	 * Name of the validator to use. If you're using PaperInputBehavior to
	 * implement your own paper-input-like element, bind this to the
	 * {@code <input is="iron-input">}'s {@code validator} property.
	 * 
	 * @param validator
	 *            the String value to set
	 * @return this instance, for method chaining
	 */
	public R setValidator(java.lang.String validator) {
		getElement().setProperty("validator",
				validator == null ? "" : validator);
		return get();
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * If you're using PaperInputBehavior to implement your own paper-input-like
	 * element, bind this to the {@code <input is="iron-input">}'s
	 * {@code autocomplete} property.
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
	 * If you're using PaperInputBehavior to implement your own paper-input-like
	 * element, bind this to the {@code <input is="iron-input">}'s
	 * {@code autocomplete} property.
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
	 * If you're using PaperInputBehavior to implement your own paper-input-like
	 * element, bind this to the {@code <input is="iron-input">}'s
	 * {@code autofocus} property.
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
	 * If you're using PaperInputBehavior to implement your own paper-input-like
	 * element, bind this to the {@code <input is="iron-input">}'s
	 * {@code autofocus} property.
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
	 * If you're using PaperInputBehavior to implement your own paper-input-like
	 * element, bind this to the {@code <input is="iron-input">}'s
	 * {@code inputmode} property.
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
	 * If you're using PaperInputBehavior to implement your own paper-input-like
	 * element, bind this to the {@code <input is="iron-input">}'s
	 * {@code inputmode} property.
	 * 
	 * @param inputmode
	 *            the String value to set
	 * @return this instance, for method chaining
	 */
	public R setInputmode(java.lang.String inputmode) {
		getElement().setProperty("inputmode",
				inputmode == null ? "" : inputmode);
		return get();
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * The minimum length of the input value. If you're using PaperInputBehavior
	 * to implement your own paper-input-like element, bind this to the
	 * {@code <input is="iron-input">}'s {@code minlength} property.
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
	 * The minimum length of the input value. If you're using PaperInputBehavior
	 * to implement your own paper-input-like element, bind this to the
	 * {@code <input is="iron-input">}'s {@code minlength} property.
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
	 * The maximum length of the input value. If you're using PaperInputBehavior
	 * to implement your own paper-input-like element, bind this to the
	 * {@code <input is="iron-input">}'s {@code maxlength} property.
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
	 * The maximum length of the input value. If you're using PaperInputBehavior
	 * to implement your own paper-input-like element, bind this to the
	 * {@code <input is="iron-input">}'s {@code maxlength} property.
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
	 * The minimum (numeric or date-time) input value. If you're using
	 * PaperInputBehavior to implement your own paper-input-like element, bind
	 * this to the {@code <input is="iron-input">}'s {@code min} property.
	 * <p>
	 * This property is not synchronized automatically from the client side, so
	 * the returned value may not be the same as in client side.
	 */
	public String getMin() {
		return getElement().getProperty("min");
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * The minimum (numeric or date-time) input value. If you're using
	 * PaperInputBehavior to implement your own paper-input-like element, bind
	 * this to the {@code <input is="iron-input">}'s {@code min} property.
	 * 
	 * @param min
	 *            the String value to set
	 * @return this instance, for method chaining
	 */
	public R setMin(java.lang.String min) {
		getElement().setProperty("min", min == null ? "" : min);
		return get();
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * The maximum (numeric or date-time) input value. Can be a String (e.g.
	 * {@code "2000-01-01"}) or a Number (e.g. {@code 2}). If you're using
	 * PaperInputBehavior to implement your own paper-input-like element, bind
	 * this to the {@code <input is="iron-input">}'s {@code max} property.
	 * <p>
	 * This property is not synchronized automatically from the client side, so
	 * the returned value may not be the same as in client side.
	 */
	public String getMax() {
		return getElement().getProperty("max");
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * The maximum (numeric or date-time) input value. Can be a String (e.g.
	 * {@code "2000-01-01"}) or a Number (e.g. {@code 2}). If you're using
	 * PaperInputBehavior to implement your own paper-input-like element, bind
	 * this to the {@code <input is="iron-input">}'s {@code max} property.
	 * 
	 * @param max
	 *            the String value to set
	 * @return this instance, for method chaining
	 */
	public R setMax(java.lang.String max) {
		getElement().setProperty("max", max == null ? "" : max);
		return get();
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Limits the numeric or date-time increments. If you're using
	 * PaperInputBehavior to implement your own paper-input-like element, bind
	 * this to the {@code <input is="iron-input">}'s {@code step} property.
	 * <p>
	 * This property is not synchronized automatically from the client side, so
	 * the returned value may not be the same as in client side.
	 */
	public String getStep() {
		return getElement().getProperty("step");
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Limits the numeric or date-time increments. If you're using
	 * PaperInputBehavior to implement your own paper-input-like element, bind
	 * this to the {@code <input is="iron-input">}'s {@code step} property.
	 * 
	 * @param step
	 *            the String value to set
	 * @return this instance, for method chaining
	 */
	public R setStep(java.lang.String step) {
		getElement().setProperty("step", step == null ? "" : step);
		return get();
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * The name of this element.
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
	 * The name of this element.
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
	 * A placeholder string in addition to the label. If this is set, the label
	 * will always float.
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
	 * A placeholder string in addition to the label. If this is set, the label
	 * will always float.
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
	 * If you're using PaperInputBehavior to implement your own paper-input-like
	 * element, bind this to the {@code <input is="iron-input">}'s
	 * {@code readonly} property.
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
	 * If you're using PaperInputBehavior to implement your own paper-input-like
	 * element, bind this to the {@code <input is="iron-input">}'s
	 * {@code readonly} property.
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
	 * If you're using PaperInputBehavior to implement your own paper-input-like
	 * element, bind this to the {@code <input is="iron-input">}'s {@code size}
	 * property.
	 * <p>
	 * This property is not synchronized automatically from the client side, so
	 * the returned value may not be the same as in client side.
	 */
	public double getSize() {
		return getElement().getProperty("size", 0.0);
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * If you're using PaperInputBehavior to implement your own paper-input-like
	 * element, bind this to the {@code <input is="iron-input">}'s {@code size}
	 * property.
	 * 
	 * @param size
	 *            the double value to set
	 * @return this instance, for method chaining
	 */
	public R setSize(double size) {
		getElement().setProperty("size", size);
		return get();
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * If you're using PaperInputBehavior to implement your own paper-input-like
	 * element, bind this to the {@code <input is="iron-input">}'s
	 * {@code autocapitalize} property.
	 * <p>
	 * This property is not synchronized automatically from the client side, so
	 * the returned value may not be the same as in client side.
	 */
	public String getAutocapitalize() {
		return getElement().getProperty("autocapitalize");
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * If you're using PaperInputBehavior to implement your own paper-input-like
	 * element, bind this to the {@code <input is="iron-input">}'s
	 * {@code autocapitalize} property.
	 * 
	 * @param autocapitalize
	 *            the String value to set
	 * @return this instance, for method chaining
	 */
	public R setAutocapitalize(java.lang.String autocapitalize) {
		getElement().setProperty("autocapitalize",
				autocapitalize == null ? "" : autocapitalize);
		return get();
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * If you're using PaperInputBehavior to implement your own paper-input-like
	 * element, bind this to the {@code <input is="iron-input">}'s
	 * {@code autocorrect} property.
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
	 * If you're using PaperInputBehavior to implement your own paper-input-like
	 * element, bind this to the {@code <input is="iron-input">}'s
	 * {@code autocorrect} property.
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
	 * If you're using PaperInputBehavior to implement your own paper-input-like
	 * element, bind this to the {@code <input is="iron-input">}'s
	 * {@code autosave} property, used with type=search.
	 * <p>
	 * This property is not synchronized automatically from the client side, so
	 * the returned value may not be the same as in client side.
	 */
	public String getAutosave() {
		return getElement().getProperty("autosave");
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * If you're using PaperInputBehavior to implement your own paper-input-like
	 * element, bind this to the {@code <input is="iron-input">}'s
	 * {@code autosave} property, used with type=search.
	 * 
	 * @param autosave
	 *            the String value to set
	 * @return this instance, for method chaining
	 */
	public R setAutosave(java.lang.String autosave) {
		getElement().setProperty("autosave", autosave == null ? "" : autosave);
		return get();
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * If you're using PaperInputBehavior to implement your own paper-input-like
	 * element, bind this to the {@code <input is="iron-input">}'s
	 * {@code results} property, used with type=search.
	 * <p>
	 * This property is not synchronized automatically from the client side, so
	 * the returned value may not be the same as in client side.
	 */
	public double getResults() {
		return getElement().getProperty("results", 0.0);
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * If you're using PaperInputBehavior to implement your own paper-input-like
	 * element, bind this to the {@code <input is="iron-input">}'s
	 * {@code results} property, used with type=search.
	 * 
	 * @param results
	 *            the double value to set
	 * @return this instance, for method chaining
	 */
	public R setResults(double results) {
		getElement().setProperty("results", results);
		return get();
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * If you're using PaperInputBehavior to implement your own paper-input-like
	 * element, bind this to the {@code <input is="iron-input">}'s
	 * {@code accept} property, used with type=file.
	 * <p>
	 * This property is not synchronized automatically from the client side, so
	 * the returned value may not be the same as in client side.
	 */
	public String getAccept() {
		return getElement().getProperty("accept");
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * If you're using PaperInputBehavior to implement your own paper-input-like
	 * element, bind this to the {@code <input is="iron-input">}'s
	 * {@code accept} property, used with type=file.
	 * 
	 * @param accept
	 *            the String value to set
	 * @return this instance, for method chaining
	 */
	public R setAccept(java.lang.String accept) {
		getElement().setProperty("accept", accept == null ? "" : accept);
		return get();
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * If you're using PaperInputBehavior to implement your own paper-input-like
	 * element, bind this to the{@code <input is="iron-input">}'s
	 * {@code multiple} property, used with type=file.
	 * <p>
	 * This property is not synchronized automatically from the client side, so
	 * the returned value may not be the same as in client side.
	 */
	public boolean isMultiple() {
		return getElement().getProperty("multiple", false);
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * If you're using PaperInputBehavior to implement your own paper-input-like
	 * element, bind this to the{@code <input is="iron-input">}'s
	 * {@code multiple} property, used with type=file.
	 * 
	 * @param multiple
	 *            the boolean value to set
	 * @return this instance, for method chaining
	 */
	public R setMultiple(boolean multiple) {
		getElement().setProperty("multiple", multiple);
		return get();
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Can be used to imperatively add a key binding to the implementing
	 * element. This is the imperative equivalent of declaring a keybinding in
	 * the {@code keyBindings} prototype property.
	 * 
	 * @param eventString
	 *            Missing documentation!
	 * @param handlerName
	 *            Missing documentation!
	 */
	public void addOwnKeyBinding(java.lang.String eventString,
			java.lang.String handlerName) {
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
	 * Returns true if a keyboard event matches {@code eventString}.
	 * 
	 * @param event
	 *            Missing documentation!
	 * @param eventString
	 *            Missing documentation!
	 * @return It would return a boolean
	 */
	@NotSupported
	protected void keyboardEventMatchesKeys(elemental.json.JsonObject event,
			java.lang.String eventString) {
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
	 * 
	 * @return It would return a boolean
	 */
	@NotSupported
	protected void validate() {
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Restores the cursor to its original position after updating the value.
	 * 
	 * @param newValue
	 *            The value that should be saved.
	 */
	public void updateValueAndPreserveCaret(java.lang.String newValue) {
		getElement().callFunction("updateValueAndPreserveCaret", newValue);
	}

	@DomEvent("focused-changed")
	public static class FocusedChangeEvent<R extends GeneratedPaperInput<R>>
			extends
				ComponentEvent<R> {
		public FocusedChangeEvent(R source, boolean fromClient) {
			super(source, fromClient);
		}
	}

	public Registration addFocusedChangeListener(
			ComponentEventListener<FocusedChangeEvent<R>> listener) {
		return addListener(FocusedChangeEvent.class,
				(ComponentEventListener) listener);
	}

	@DomEvent("disabled-changed")
	public static class DisabledChangeEvent<R extends GeneratedPaperInput<R>>
			extends
				ComponentEvent<R> {
		public DisabledChangeEvent(R source, boolean fromClient) {
			super(source, fromClient);
		}
	}

	public Registration addDisabledChangeListener(
			ComponentEventListener<DisabledChangeEvent<R>> listener) {
		return addListener(DisabledChangeEvent.class,
				(ComponentEventListener) listener);
	}

	@DomEvent("change")
	public static class ChangeEvent<R extends GeneratedPaperInput<R>>
			extends
				ComponentEvent<R> {
		public ChangeEvent(R source, boolean fromClient) {
			super(source, fromClient);
		}
	}

	public Registration addChangeListener(
			ComponentEventListener<ChangeEvent<R>> listener) {
		return addListener(ChangeEvent.class, (ComponentEventListener) listener);
	}

	@DomEvent("invalid-changed")
	public static class InvalidChangeEvent<R extends GeneratedPaperInput<R>>
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

	@DomEvent("iron-form-element-register")
	public static class IronFormElementRegisterEvent<R extends GeneratedPaperInput<R>>
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
	public static class IronFormElementUnregisterEvent<R extends GeneratedPaperInput<R>>
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