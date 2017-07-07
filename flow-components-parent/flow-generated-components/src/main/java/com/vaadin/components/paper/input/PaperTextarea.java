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
package com.vaadin.components.paper.input;

import com.vaadin.ui.Component;
import com.vaadin.ui.HasStyle;
import javax.annotation.Generated;
import com.vaadin.annotations.Tag;
import com.vaadin.annotations.HtmlImport;
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
 * {@code <paper-textarea>} is a multi-line text field with Material Design
 * styling.
 * 
 * <paper-textarea label="Textarea label"></paper-textarea>
 * 
 * See {@code Polymer.PaperInputBehavior} for more API docs.
 * 
 * ### Validation
 * 
 * Currently only {@code required} and {@code maxlength} validation is
 * supported.
 * 
 * ### Styling
 * 
 * See {@code Polymer.PaperInputContainer} for a list of custom properties used
 * to style this element.
 */
@Generated({
		"Generator: com.vaadin.generator.ComponentGenerator#0.1.12-SNAPSHOT",
		"WebComponent: paper-textarea#2.0.1", "Flow#0.1.12-SNAPSHOT"})
@Tag("paper-textarea")
@HtmlImport("frontend://bower_components/paper-input/paper-textarea.html")
public class PaperTextarea<R extends PaperTextarea<R>> extends Component
		implements
			HasStyle {

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
	 * If true, the element currently has focus.
	 * 
	 * @param focused
	 * @return this instance, for method chaining
	 */
	public R setFocused(boolean focused) {
		getElement().setProperty("focused", focused);
		return getSelf();
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
	 * @return this instance, for method chaining
	 */
	public R setDisabled(boolean disabled) {
		getElement().setProperty("disabled", disabled);
		return getSelf();
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
	public JsonObject getKeyEventTarget() {
		return (JsonObject) getElement().getPropertyRaw("keyEventTarget");
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * The EventTarget that will be firing relevant KeyboardEvents. Set it to
	 * {@code null} to disable the listeners.
	 * 
	 * @param keyEventTarget
	 * @return this instance, for method chaining
	 */
	public R setKeyEventTarget(elemental.json.JsonObject keyEventTarget) {
		getElement().setPropertyJson("keyEventTarget", keyEventTarget);
		return getSelf();
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
	 * @return this instance, for method chaining
	 */
	public R setStopKeyboardEventPropagation(
			boolean stopKeyboardEventPropagation) {
		getElement().setProperty("stopKeyboardEventPropagation",
				stopKeyboardEventPropagation);
		return getSelf();
	}

	/**
	 * This property is not synchronized automatically from the client side, so
	 * the returned value may not be the same as in client side.
	 */
	public JsonObject getKeyBindings() {
		return (JsonObject) getElement().getPropertyRaw("keyBindings");
	}

	/**
	 * @param keyBindings
	 * @return this instance, for method chaining
	 */
	public R setKeyBindings(elemental.json.JsonObject keyBindings) {
		getElement().setPropertyJson("keyBindings", keyBindings);
		return getSelf();
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
	 * @return this instance, for method chaining
	 */
	public R setLabel(java.lang.String label) {
		getElement().setProperty("label", label == null ? "" : label);
		return getSelf();
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
	public String getValue() {
		return getElement().getProperty("value");
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * The value for this element.
	 * 
	 * @param value
	 * @return this instance, for method chaining
	 */
	public R setValue(java.lang.String value) {
		getElement().setProperty("value", value == null ? "" : value);
		return getSelf();
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
	 * @return this instance, for method chaining
	 */
	public R setInvalid(boolean invalid) {
		getElement().setProperty("invalid", invalid);
		return getSelf();
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
	 * @return this instance, for method chaining
	 */
	public R setAllowedPattern(java.lang.String allowedPattern) {
		getElement().setProperty("allowedPattern",
				allowedPattern == null ? "" : allowedPattern);
		return getSelf();
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
	 * @return this instance, for method chaining
	 */
	public R setType(java.lang.String type) {
		getElement().setProperty("type", type == null ? "" : type);
		return getSelf();
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
	 * @return this instance, for method chaining
	 */
	public R setList(java.lang.String list) {
		getElement().setProperty("list", list == null ? "" : list);
		return getSelf();
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
	 * @return this instance, for method chaining
	 */
	public R setPattern(java.lang.String pattern) {
		getElement().setProperty("pattern", pattern == null ? "" : pattern);
		return getSelf();
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
	 * @return this instance, for method chaining
	 */
	public R setRequired(boolean required) {
		getElement().setProperty("required", required);
		return getSelf();
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
	 * @return this instance, for method chaining
	 */
	public R setErrorMessage(java.lang.String errorMessage) {
		getElement().setProperty("errorMessage",
				errorMessage == null ? "" : errorMessage);
		return getSelf();
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
	 * @return this instance, for method chaining
	 */
	public R setCharCounter(boolean charCounter) {
		getElement().setProperty("charCounter", charCounter);
		return getSelf();
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
	 * @return this instance, for method chaining
	 */
	public R setNoLabelFloat(boolean noLabelFloat) {
		getElement().setProperty("noLabelFloat", noLabelFloat);
		return getSelf();
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
	 * @return this instance, for method chaining
	 */
	public R setAlwaysFloatLabel(boolean alwaysFloatLabel) {
		getElement().setProperty("alwaysFloatLabel", alwaysFloatLabel);
		return getSelf();
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
	 * @return this instance, for method chaining
	 */
	public R setAutoValidate(boolean autoValidate) {
		getElement().setProperty("autoValidate", autoValidate);
		return getSelf();
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
	 * @return this instance, for method chaining
	 */
	public R setValidator(java.lang.String validator) {
		getElement().setProperty("validator",
				validator == null ? "" : validator);
		return getSelf();
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
	 * @return this instance, for method chaining
	 */
	public R setAutocomplete(java.lang.String autocomplete) {
		getElement().setProperty("autocomplete",
				autocomplete == null ? "" : autocomplete);
		return getSelf();
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
	 * @return this instance, for method chaining
	 */
	public R setAutofocus(boolean autofocus) {
		getElement().setProperty("autofocus", autofocus);
		return getSelf();
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
	 * @return this instance, for method chaining
	 */
	public R setInputmode(java.lang.String inputmode) {
		getElement().setProperty("inputmode",
				inputmode == null ? "" : inputmode);
		return getSelf();
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
	 * @return this instance, for method chaining
	 */
	public R setMinlength(double minlength) {
		getElement().setProperty("minlength", minlength);
		return getSelf();
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
	 * @return this instance, for method chaining
	 */
	public R setMaxlength(double maxlength) {
		getElement().setProperty("maxlength", maxlength);
		return getSelf();
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
	 * @return this instance, for method chaining
	 */
	public R setMin(java.lang.String min) {
		getElement().setProperty("min", min == null ? "" : min);
		return getSelf();
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
	 * @return this instance, for method chaining
	 */
	public R setMax(java.lang.String max) {
		getElement().setProperty("max", max == null ? "" : max);
		return getSelf();
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
	 * @return this instance, for method chaining
	 */
	public R setStep(java.lang.String step) {
		getElement().setProperty("step", step == null ? "" : step);
		return getSelf();
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
	 * @return this instance, for method chaining
	 */
	public R setName(java.lang.String name) {
		getElement().setProperty("name", name == null ? "" : name);
		return getSelf();
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
	 * @return this instance, for method chaining
	 */
	public R setPlaceholder(java.lang.String placeholder) {
		getElement().setProperty("placeholder",
				placeholder == null ? "" : placeholder);
		return getSelf();
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
	 * @return this instance, for method chaining
	 */
	public R setReadonly(boolean readonly) {
		getElement().setProperty("readonly", readonly);
		return getSelf();
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
	 * @return this instance, for method chaining
	 */
	public R setSize(double size) {
		getElement().setProperty("size", size);
		return getSelf();
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
	 * @return this instance, for method chaining
	 */
	public R setAutocapitalize(java.lang.String autocapitalize) {
		getElement().setProperty("autocapitalize",
				autocapitalize == null ? "" : autocapitalize);
		return getSelf();
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
	 * @return this instance, for method chaining
	 */
	public R setAutocorrect(java.lang.String autocorrect) {
		getElement().setProperty("autocorrect",
				autocorrect == null ? "" : autocorrect);
		return getSelf();
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
	 * @return this instance, for method chaining
	 */
	public R setAutosave(java.lang.String autosave) {
		getElement().setProperty("autosave", autosave == null ? "" : autosave);
		return getSelf();
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
	 * @return this instance, for method chaining
	 */
	public R setResults(double results) {
		getElement().setProperty("results", results);
		return getSelf();
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
	 * @return this instance, for method chaining
	 */
	public R setAccept(java.lang.String accept) {
		getElement().setProperty("accept", accept == null ? "" : accept);
		return getSelf();
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
	 * @return this instance, for method chaining
	 */
	public R setMultiple(boolean multiple) {
		getElement().setProperty("multiple", multiple);
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
	 * @return this instance, for method chaining
	 */
	public R setRows(double rows) {
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
	 * @return this instance, for method chaining
	 */
	public R setMaxRows(double maxRows) {
		getElement().setProperty("maxRows", maxRows);
		return getSelf();
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Can be used to imperatively add a key binding to the implementing
	 * element. This is the imperative equivalent of declaring a keybinding in
	 * the {@code keyBindings} prototype property.
	 * 
	 * @param eventString
	 * @param handlerName
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
	 * @param eventString
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
	 */
	public void updateValueAndPreserveCaret(java.lang.String newValue) {
		getElement().callFunction("updateValueAndPreserveCaret", newValue);
	}

	@DomEvent("focused-changed")
	public static class FocusedChangedEvent
			extends
				ComponentEvent<PaperTextarea> {
		public FocusedChangedEvent(PaperTextarea source, boolean fromClient) {
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
				ComponentEvent<PaperTextarea> {
		public DisabledChangedEvent(PaperTextarea source, boolean fromClient) {
			super(source, fromClient);
		}
	}

	public Registration addDisabledChangedListener(
			ComponentEventListener<DisabledChangedEvent> listener) {
		return addListener(DisabledChangedEvent.class, listener);
	}

	@DomEvent("change")
	public static class ChangeEvent extends ComponentEvent<PaperTextarea> {
		public ChangeEvent(PaperTextarea source, boolean fromClient) {
			super(source, fromClient);
		}
	}

	public Registration addChangeListener(
			ComponentEventListener<ChangeEvent> listener) {
		return addListener(ChangeEvent.class, listener);
	}

	@DomEvent("value-changed")
	public static class ValueChangedEvent extends ComponentEvent<PaperTextarea> {
		public ValueChangedEvent(PaperTextarea source, boolean fromClient) {
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
				ComponentEvent<PaperTextarea> {
		public InvalidChangedEvent(PaperTextarea source, boolean fromClient) {
			super(source, fromClient);
		}
	}

	public Registration addInvalidChangedListener(
			ComponentEventListener<InvalidChangedEvent> listener) {
		return addListener(InvalidChangedEvent.class, listener);
	}

	@DomEvent("iron-form-element-register")
	public static class IronFormElementRegisterEvent
			extends
				ComponentEvent<PaperTextarea> {
		public IronFormElementRegisterEvent(PaperTextarea source,
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
				ComponentEvent<PaperTextarea> {
		public IronFormElementUnregisterEvent(PaperTextarea source,
				boolean fromClient) {
			super(source, fromClient);
		}
	}

	public Registration addIronFormElementUnregisterListener(
			ComponentEventListener<IronFormElementUnregisterEvent> listener) {
		return addListener(IronFormElementUnregisterEvent.class, listener);
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