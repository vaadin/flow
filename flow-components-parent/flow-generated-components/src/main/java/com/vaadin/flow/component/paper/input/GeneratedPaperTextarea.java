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
package com.vaadin.flow.component.paper.input;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasStyle;
import com.vaadin.flow.component.ComponentSupplier;
import javax.annotation.Generated;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.dependency.HtmlImport;
import com.vaadin.flow.component.Synchronize;
import elemental.json.JsonObject;
import com.vaadin.flow.component.NotSupported;
import com.vaadin.flow.component.DomEvent;
import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.shared.Registration;

@Generated({ "Generator: com.vaadin.generator.ComponentGenerator#1.0-SNAPSHOT",
        "WebComponent: paper-textarea#2.0.2", "Flow#1.0-SNAPSHOT" })
@Tag("paper-textarea")
@HtmlImport("frontend://bower_components/paper-input/paper-textarea.html")
public abstract class GeneratedPaperTextarea<R extends GeneratedPaperTextarea<R>>
        extends Component implements HasStyle, ComponentSupplier<R> {

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * If true, the element currently has focus.
     * <p>
     * This property is synchronized automatically from client side when a
     * 'focused-changed' event happens.
     * </p>
     * 
     * @return the {@code focused} property from the webcomponent
     */
    @Synchronize(property = "focused", value = "focused-changed")
    protected boolean isFocusedBoolean() {
        return getElement().getProperty("focused", false);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * Set to true to disable this input. If you're using PaperInputBehavior to
     * implement your own paper-input-like element, bind this to both the
     * {@code <paper-input-container>}'s and the input's {@code disabled}
     * property.
     * <p>
     * This property is synchronized automatically from client side when a
     * 'disabled-changed' event happens.
     * </p>
     * 
     * @return the {@code disabled} property from the webcomponent
     */
    @Synchronize(property = "disabled", value = "disabled-changed")
    protected boolean isDisabledBoolean() {
        return getElement().getProperty("disabled", false);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * Set to true to disable this input. If you're using PaperInputBehavior to
     * implement your own paper-input-like element, bind this to both the
     * {@code <paper-input-container>}'s and the input's {@code disabled}
     * property.
     * </p>
     * 
     * @param disabled
     *            the boolean value to set
     */
    protected void setDisabled(boolean disabled) {
        getElement().setProperty("disabled", disabled);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * The EventTarget that will be firing relevant KeyboardEvents. Set it to
     * {@code null} to disable the listeners.
     * <p>
     * This property is not synchronized automatically from the client side, so
     * the returned value may not be the same as in client side.
     * </p>
     * 
     * @return the {@code keyEventTarget} property from the webcomponent
     */
    protected JsonObject getKeyEventTargetJsonObject() {
        return (JsonObject) getElement().getPropertyRaw("keyEventTarget");
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * The EventTarget that will be firing relevant KeyboardEvents. Set it to
     * {@code null} to disable the listeners.
     * </p>
     * 
     * @param keyEventTarget
     *            the JsonObject value to set
     */
    protected void setKeyEventTarget(JsonObject keyEventTarget) {
        getElement().setPropertyJson("keyEventTarget", keyEventTarget);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * If true, this property will cause the implementing element to
     * automatically stop propagation on any handled KeyboardEvents.
     * <p>
     * This property is not synchronized automatically from the client side, so
     * the returned value may not be the same as in client side.
     * </p>
     * 
     * @return the {@code stopKeyboardEventPropagation} property from the
     *         webcomponent
     */
    protected boolean isStopKeyboardEventPropagationBoolean() {
        return getElement().getProperty("stopKeyboardEventPropagation", false);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * If true, this property will cause the implementing element to
     * automatically stop propagation on any handled KeyboardEvents.
     * </p>
     * 
     * @param stopKeyboardEventPropagation
     *            the boolean value to set
     */
    protected void setStopKeyboardEventPropagation(
            boolean stopKeyboardEventPropagation) {
        getElement().setProperty("stopKeyboardEventPropagation",
                stopKeyboardEventPropagation);
    }

    /**
     * This property is not synchronized automatically from the client side, so
     * the returned value may not be the same as in client side.
     * 
     * @return the {@code keyBindings} property from the webcomponent
     */
    protected JsonObject getKeyBindingsJsonObject() {
        return (JsonObject) getElement().getPropertyRaw("keyBindings");
    }

    /**
     * @param keyBindings
     *            the JsonObject value to set
     */
    protected void setKeyBindings(JsonObject keyBindings) {
        getElement().setPropertyJson("keyBindings", keyBindings);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * The label for this input. If you're using PaperInputBehavior to implement
     * your own paper-input-like element, bind this to {@code <label>}'s content
     * and {@code hidden} property, e.g.
     * {@code &lt;label hidden$=&quot;[[!label]]&quot;&gt;[[label]]</label>} in
     * your {@code template}
     * <p>
     * This property is not synchronized automatically from the client side, so
     * the returned value may not be the same as in client side.
     * </p>
     * 
     * @return the {@code label} property from the webcomponent
     */
    protected String getLabelString() {
        return getElement().getProperty("label");
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * The label for this input. If you're using PaperInputBehavior to implement
     * your own paper-input-like element, bind this to {@code <label>}'s content
     * and {@code hidden} property, e.g.
     * {@code &lt;label hidden$=&quot;[[!label]]&quot;&gt;[[label]]</label>} in
     * your {@code template}
     * </p>
     * 
     * @param label
     *            the String value to set
     */
    protected void setLabel(String label) {
        getElement().setProperty("label", label == null ? "" : label);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * The value for this element.
     * <p>
     * This property is synchronized automatically from client side when a
     * 'value-changed' event happens.
     * </p>
     * 
     * @return the {@code value} property from the webcomponent
     */
    @Synchronize(property = "value", value = "value-changed")
    protected String getValueString() {
        return getElement().getProperty("value");
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * The value for this element.
     * </p>
     * 
     * @param value
     *            the String value to set
     */
    protected void setValue(String value) {
        getElement().setProperty("value", value == null ? "" : value);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * Returns true if the value is invalid. If you're using PaperInputBehavior
     * to implement your own paper-input-like element, bind this to both the
     * {@code <paper-input-container>}'s and the input's {@code invalid}
     * property.
     * </p>
     * <p>
     * If {@code autoValidate} is true, the {@code invalid} attribute is managed
     * automatically, which can clobber attempts to manage it manually.
     * <p>
     * This property is synchronized automatically from client side when a
     * 'invalid-changed' event happens.
     * </p>
     * 
     * @return the {@code invalid} property from the webcomponent
     */
    @Synchronize(property = "invalid", value = "invalid-changed")
    protected boolean isInvalidBoolean() {
        return getElement().getProperty("invalid", false);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * Returns true if the value is invalid. If you're using PaperInputBehavior
     * to implement your own paper-input-like element, bind this to both the
     * {@code <paper-input-container>}'s and the input's {@code invalid}
     * property.
     * </p>
     * <p>
     * If {@code autoValidate} is true, the {@code invalid} attribute is managed
     * automatically, which can clobber attempts to manage it manually.
     * </p>
     * 
     * @param invalid
     *            the boolean value to set
     */
    protected void setInvalid(boolean invalid) {
        getElement().setProperty("invalid", invalid);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * Set this to specify the pattern allowed by {@code preventInvalidInput}.
     * If you're using PaperInputBehavior to implement your own paper-input-like
     * element, bind this to the {@code <input is="iron-input">}'s
     * {@code allowedPattern} property.
     * <p>
     * This property is not synchronized automatically from the client side, so
     * the returned value may not be the same as in client side.
     * </p>
     * 
     * @return the {@code allowedPattern} property from the webcomponent
     */
    protected String getAllowedPatternString() {
        return getElement().getProperty("allowedPattern");
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * Set this to specify the pattern allowed by {@code preventInvalidInput}.
     * If you're using PaperInputBehavior to implement your own paper-input-like
     * element, bind this to the {@code <input is="iron-input">}'s
     * {@code allowedPattern} property.
     * </p>
     * 
     * @param allowedPattern
     *            the String value to set
     */
    protected void setAllowedPattern(String allowedPattern) {
        getElement().setProperty("allowedPattern",
                allowedPattern == null ? "" : allowedPattern);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * The type of the input. The supported types are the <a href=
     * "https://developer.mozilla.org/en-US/docs/Web/HTML/Element/input#Form_&lt;input&gt;_types"
     * >native input's types</a>. If you're using PaperInputBehavior to
     * implement your own paper-input-like element, bind this to the (Polymer 1)
     * {@code <input is="iron-input">}'s or (Polymer 2) {@code <iron-input>}'s
     * {@code type} property.
     * <p>
     * This property is not synchronized automatically from the client side, so
     * the returned value may not be the same as in client side.
     * </p>
     * 
     * @return the {@code type} property from the webcomponent
     */
    protected String getTypeString() {
        return getElement().getProperty("type");
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * The type of the input. The supported types are the <a href=
     * "https://developer.mozilla.org/en-US/docs/Web/HTML/Element/input#Form_&lt;input&gt;_types"
     * >native input's types</a>. If you're using PaperInputBehavior to
     * implement your own paper-input-like element, bind this to the (Polymer 1)
     * {@code <input is="iron-input">}'s or (Polymer 2) {@code <iron-input>}'s
     * {@code type} property.
     * </p>
     * 
     * @param type
     *            the String value to set
     */
    protected void setType(String type) {
        getElement().setProperty("type", type == null ? "" : type);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * The datalist of the input (if any). This should match the id of an
     * existing {@code <datalist>}. If you're using PaperInputBehavior to
     * implement your own paper-input-like element, bind this to the
     * {@code <input is="iron-input">}'s {@code list} property.
     * <p>
     * This property is not synchronized automatically from the client side, so
     * the returned value may not be the same as in client side.
     * </p>
     * 
     * @return the {@code list} property from the webcomponent
     */
    protected String getListString() {
        return getElement().getProperty("list");
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * The datalist of the input (if any). This should match the id of an
     * existing {@code <datalist>}. If you're using PaperInputBehavior to
     * implement your own paper-input-like element, bind this to the
     * {@code <input is="iron-input">}'s {@code list} property.
     * </p>
     * 
     * @param list
     *            the String value to set
     */
    protected void setList(String list) {
        getElement().setProperty("list", list == null ? "" : list);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * A pattern to validate the {@code input} with. If you're using
     * PaperInputBehavior to implement your own paper-input-like element, bind
     * this to the {@code <input is="iron-input">}'s {@code pattern} property.
     * <p>
     * This property is not synchronized automatically from the client side, so
     * the returned value may not be the same as in client side.
     * </p>
     * 
     * @return the {@code pattern} property from the webcomponent
     */
    protected String getPatternString() {
        return getElement().getProperty("pattern");
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * A pattern to validate the {@code input} with. If you're using
     * PaperInputBehavior to implement your own paper-input-like element, bind
     * this to the {@code <input is="iron-input">}'s {@code pattern} property.
     * </p>
     * 
     * @param pattern
     *            the String value to set
     */
    protected void setPattern(String pattern) {
        getElement().setProperty("pattern", pattern == null ? "" : pattern);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * Set to true to mark the input as required. If used in a form, a custom
     * element that uses this behavior should also use
     * Polymer.IronValidatableBehavior and define a custom validation method.
     * Otherwise, a {@code required} element will always be considered valid.
     * It's also strongly recommended to provide a visual style for the element
     * when its value is invalid.
     * <p>
     * This property is not synchronized automatically from the client side, so
     * the returned value may not be the same as in client side.
     * </p>
     * 
     * @return the {@code required} property from the webcomponent
     */
    protected boolean isRequiredBoolean() {
        return getElement().getProperty("required", false);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * Set to true to mark the input as required. If used in a form, a custom
     * element that uses this behavior should also use
     * Polymer.IronValidatableBehavior and define a custom validation method.
     * Otherwise, a {@code required} element will always be considered valid.
     * It's also strongly recommended to provide a visual style for the element
     * when its value is invalid.
     * </p>
     * 
     * @param required
     *            the boolean value to set
     */
    protected void setRequired(boolean required) {
        getElement().setProperty("required", required);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * The error message to display when the input is invalid. If you're using
     * PaperInputBehavior to implement your own paper-input-like element, bind
     * this to the {@code <paper-input-error>}'s content, if using.
     * <p>
     * This property is not synchronized automatically from the client side, so
     * the returned value may not be the same as in client side.
     * </p>
     * 
     * @return the {@code errorMessage} property from the webcomponent
     */
    protected String getErrorMessageString() {
        return getElement().getProperty("errorMessage");
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * The error message to display when the input is invalid. If you're using
     * PaperInputBehavior to implement your own paper-input-like element, bind
     * this to the {@code <paper-input-error>}'s content, if using.
     * </p>
     * 
     * @param errorMessage
     *            the String value to set
     */
    protected void setErrorMessage(String errorMessage) {
        getElement().setProperty("errorMessage",
                errorMessage == null ? "" : errorMessage);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * Set to true to show a character counter.
     * <p>
     * This property is not synchronized automatically from the client side, so
     * the returned value may not be the same as in client side.
     * </p>
     * 
     * @return the {@code charCounter} property from the webcomponent
     */
    protected boolean isCharCounterBoolean() {
        return getElement().getProperty("charCounter", false);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * Set to true to show a character counter.
     * </p>
     * 
     * @param charCounter
     *            the boolean value to set
     */
    protected void setCharCounter(boolean charCounter) {
        getElement().setProperty("charCounter", charCounter);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * Set to true to disable the floating label. If you're using
     * PaperInputBehavior to implement your own paper-input-like element, bind
     * this to the {@code <paper-input-container>}'s {@code noLabelFloat}
     * property.
     * <p>
     * This property is not synchronized automatically from the client side, so
     * the returned value may not be the same as in client side.
     * </p>
     * 
     * @return the {@code noLabelFloat} property from the webcomponent
     */
    protected boolean isNoLabelFloatBoolean() {
        return getElement().getProperty("noLabelFloat", false);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * Set to true to disable the floating label. If you're using
     * PaperInputBehavior to implement your own paper-input-like element, bind
     * this to the {@code <paper-input-container>}'s {@code noLabelFloat}
     * property.
     * </p>
     * 
     * @param noLabelFloat
     *            the boolean value to set
     */
    protected void setNoLabelFloat(boolean noLabelFloat) {
        getElement().setProperty("noLabelFloat", noLabelFloat);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * Set to true to always float the label. If you're using PaperInputBehavior
     * to implement your own paper-input-like element, bind this to the
     * {@code <paper-input-container>}'s {@code alwaysFloatLabel} property.
     * <p>
     * This property is not synchronized automatically from the client side, so
     * the returned value may not be the same as in client side.
     * </p>
     * 
     * @return the {@code alwaysFloatLabel} property from the webcomponent
     */
    protected boolean isAlwaysFloatLabelBoolean() {
        return getElement().getProperty("alwaysFloatLabel", false);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * Set to true to always float the label. If you're using PaperInputBehavior
     * to implement your own paper-input-like element, bind this to the
     * {@code <paper-input-container>}'s {@code alwaysFloatLabel} property.
     * </p>
     * 
     * @param alwaysFloatLabel
     *            the boolean value to set
     */
    protected void setAlwaysFloatLabel(boolean alwaysFloatLabel) {
        getElement().setProperty("alwaysFloatLabel", alwaysFloatLabel);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * Set to true to auto-validate the input value. If you're using
     * PaperInputBehavior to implement your own paper-input-like element, bind
     * this to the {@code <paper-input-container>}'s {@code autoValidate}
     * property.
     * <p>
     * This property is not synchronized automatically from the client side, so
     * the returned value may not be the same as in client side.
     * </p>
     * 
     * @return the {@code autoValidate} property from the webcomponent
     */
    protected boolean isAutoValidateBoolean() {
        return getElement().getProperty("autoValidate", false);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * Set to true to auto-validate the input value. If you're using
     * PaperInputBehavior to implement your own paper-input-like element, bind
     * this to the {@code <paper-input-container>}'s {@code autoValidate}
     * property.
     * </p>
     * 
     * @param autoValidate
     *            the boolean value to set
     */
    protected void setAutoValidate(boolean autoValidate) {
        getElement().setProperty("autoValidate", autoValidate);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * Name of the validator to use. If you're using PaperInputBehavior to
     * implement your own paper-input-like element, bind this to the
     * {@code <input is="iron-input">}'s {@code validator} property.
     * <p>
     * This property is not synchronized automatically from the client side, so
     * the returned value may not be the same as in client side.
     * </p>
     * 
     * @return the {@code validator} property from the webcomponent
     */
    protected String getValidatorString() {
        return getElement().getProperty("validator");
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * Name of the validator to use. If you're using PaperInputBehavior to
     * implement your own paper-input-like element, bind this to the
     * {@code <input is="iron-input">}'s {@code validator} property.
     * </p>
     * 
     * @param validator
     *            the String value to set
     */
    protected void setValidator(String validator) {
        getElement().setProperty("validator",
                validator == null ? "" : validator);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * If you're using PaperInputBehavior to implement your own paper-input-like
     * element, bind this to the {@code <input is="iron-input">}'s
     * {@code autocomplete} property.
     * <p>
     * This property is not synchronized automatically from the client side, so
     * the returned value may not be the same as in client side.
     * </p>
     * 
     * @return the {@code autocomplete} property from the webcomponent
     */
    protected String getAutocompleteString() {
        return getElement().getProperty("autocomplete");
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * If you're using PaperInputBehavior to implement your own paper-input-like
     * element, bind this to the {@code <input is="iron-input">}'s
     * {@code autocomplete} property.
     * </p>
     * 
     * @param autocomplete
     *            the String value to set
     */
    protected void setAutocomplete(String autocomplete) {
        getElement().setProperty("autocomplete",
                autocomplete == null ? "" : autocomplete);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * If you're using PaperInputBehavior to implement your own paper-input-like
     * element, bind this to the {@code <input is="iron-input">}'s
     * {@code autofocus} property.
     * <p>
     * This property is not synchronized automatically from the client side, so
     * the returned value may not be the same as in client side.
     * </p>
     * 
     * @return the {@code autofocus} property from the webcomponent
     */
    protected boolean isAutofocusBoolean() {
        return getElement().getProperty("autofocus", false);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * If you're using PaperInputBehavior to implement your own paper-input-like
     * element, bind this to the {@code <input is="iron-input">}'s
     * {@code autofocus} property.
     * </p>
     * 
     * @param autofocus
     *            the boolean value to set
     */
    protected void setAutofocus(boolean autofocus) {
        getElement().setProperty("autofocus", autofocus);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * If you're using PaperInputBehavior to implement your own paper-input-like
     * element, bind this to the {@code <input is="iron-input">}'s
     * {@code inputmode} property.
     * <p>
     * This property is not synchronized automatically from the client side, so
     * the returned value may not be the same as in client side.
     * </p>
     * 
     * @return the {@code inputmode} property from the webcomponent
     */
    protected String getInputmodeString() {
        return getElement().getProperty("inputmode");
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * If you're using PaperInputBehavior to implement your own paper-input-like
     * element, bind this to the {@code <input is="iron-input">}'s
     * {@code inputmode} property.
     * </p>
     * 
     * @param inputmode
     *            the String value to set
     */
    protected void setInputmode(String inputmode) {
        getElement().setProperty("inputmode",
                inputmode == null ? "" : inputmode);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * The minimum length of the input value. If you're using PaperInputBehavior
     * to implement your own paper-input-like element, bind this to the
     * {@code <input is="iron-input">}'s {@code minlength} property.
     * <p>
     * This property is not synchronized automatically from the client side, so
     * the returned value may not be the same as in client side.
     * </p>
     * 
     * @return the {@code minlength} property from the webcomponent
     */
    protected double getMinlengthDouble() {
        return getElement().getProperty("minlength", 0.0);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * The minimum length of the input value. If you're using PaperInputBehavior
     * to implement your own paper-input-like element, bind this to the
     * {@code <input is="iron-input">}'s {@code minlength} property.
     * </p>
     * 
     * @param minlength
     *            the double value to set
     */
    protected void setMinlength(double minlength) {
        getElement().setProperty("minlength", minlength);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * The maximum length of the input value. If you're using PaperInputBehavior
     * to implement your own paper-input-like element, bind this to the
     * {@code <input is="iron-input">}'s {@code maxlength} property.
     * <p>
     * This property is not synchronized automatically from the client side, so
     * the returned value may not be the same as in client side.
     * </p>
     * 
     * @return the {@code maxlength} property from the webcomponent
     */
    protected double getMaxlengthDouble() {
        return getElement().getProperty("maxlength", 0.0);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * The maximum length of the input value. If you're using PaperInputBehavior
     * to implement your own paper-input-like element, bind this to the
     * {@code <input is="iron-input">}'s {@code maxlength} property.
     * </p>
     * 
     * @param maxlength
     *            the double value to set
     */
    protected void setMaxlength(double maxlength) {
        getElement().setProperty("maxlength", maxlength);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * The minimum (numeric or date-time) input value. If you're using
     * PaperInputBehavior to implement your own paper-input-like element, bind
     * this to the {@code <input is="iron-input">}'s {@code min} property.
     * <p>
     * This property is not synchronized automatically from the client side, so
     * the returned value may not be the same as in client side.
     * </p>
     * 
     * @return the {@code min} property from the webcomponent
     */
    protected String getMinString() {
        return getElement().getProperty("min");
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * The minimum (numeric or date-time) input value. If you're using
     * PaperInputBehavior to implement your own paper-input-like element, bind
     * this to the {@code <input is="iron-input">}'s {@code min} property.
     * </p>
     * 
     * @param min
     *            the String value to set
     */
    protected void setMin(String min) {
        getElement().setProperty("min", min == null ? "" : min);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * The maximum (numeric or date-time) input value. Can be a String (e.g.
     * {@code &quot;2000-01-01&quot;}) or a Number (e.g. {@code 2}). If you're
     * using PaperInputBehavior to implement your own paper-input-like element,
     * bind this to the {@code <input is="iron-input">}'s {@code max} property.
     * <p>
     * This property is not synchronized automatically from the client side, so
     * the returned value may not be the same as in client side.
     * </p>
     * 
     * @return the {@code max} property from the webcomponent
     */
    protected String getMaxString() {
        return getElement().getProperty("max");
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * The maximum (numeric or date-time) input value. Can be a String (e.g.
     * {@code &quot;2000-01-01&quot;}) or a Number (e.g. {@code 2}). If you're
     * using PaperInputBehavior to implement your own paper-input-like element,
     * bind this to the {@code <input is="iron-input">}'s {@code max} property.
     * </p>
     * 
     * @param max
     *            the String value to set
     */
    protected void setMax(String max) {
        getElement().setProperty("max", max == null ? "" : max);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * Limits the numeric or date-time increments. If you're using
     * PaperInputBehavior to implement your own paper-input-like element, bind
     * this to the {@code <input is="iron-input">}'s {@code step} property.
     * <p>
     * This property is not synchronized automatically from the client side, so
     * the returned value may not be the same as in client side.
     * </p>
     * 
     * @return the {@code step} property from the webcomponent
     */
    protected String getStepString() {
        return getElement().getProperty("step");
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * Limits the numeric or date-time increments. If you're using
     * PaperInputBehavior to implement your own paper-input-like element, bind
     * this to the {@code <input is="iron-input">}'s {@code step} property.
     * </p>
     * 
     * @param step
     *            the String value to set
     */
    protected void setStep(String step) {
        getElement().setProperty("step", step == null ? "" : step);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * The name of this element.
     * <p>
     * This property is not synchronized automatically from the client side, so
     * the returned value may not be the same as in client side.
     * </p>
     * 
     * @return the {@code name} property from the webcomponent
     */
    protected String getNameString() {
        return getElement().getProperty("name");
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * The name of this element.
     * </p>
     * 
     * @param name
     *            the String value to set
     */
    protected void setName(String name) {
        getElement().setProperty("name", name == null ? "" : name);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * A placeholder string in addition to the label. If this is set, the label
     * will always float.
     * <p>
     * This property is not synchronized automatically from the client side, so
     * the returned value may not be the same as in client side.
     * </p>
     * 
     * @return the {@code placeholder} property from the webcomponent
     */
    protected String getPlaceholderString() {
        return getElement().getProperty("placeholder");
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * A placeholder string in addition to the label. If this is set, the label
     * will always float.
     * </p>
     * 
     * @param placeholder
     *            the String value to set
     */
    protected void setPlaceholder(String placeholder) {
        getElement().setProperty("placeholder",
                placeholder == null ? "" : placeholder);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * If you're using PaperInputBehavior to implement your own paper-input-like
     * element, bind this to the {@code <input is="iron-input">}'s
     * {@code readonly} property.
     * <p>
     * This property is not synchronized automatically from the client side, so
     * the returned value may not be the same as in client side.
     * </p>
     * 
     * @return the {@code readonly} property from the webcomponent
     */
    protected boolean isReadonlyBoolean() {
        return getElement().getProperty("readonly", false);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * If you're using PaperInputBehavior to implement your own paper-input-like
     * element, bind this to the {@code <input is="iron-input">}'s
     * {@code readonly} property.
     * </p>
     * 
     * @param readonly
     *            the boolean value to set
     */
    protected void setReadonly(boolean readonly) {
        getElement().setProperty("readonly", readonly);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * If you're using PaperInputBehavior to implement your own paper-input-like
     * element, bind this to the {@code <input is="iron-input">}'s {@code size}
     * property.
     * <p>
     * This property is not synchronized automatically from the client side, so
     * the returned value may not be the same as in client side.
     * </p>
     * 
     * @return the {@code size} property from the webcomponent
     */
    protected double getSizeDouble() {
        return getElement().getProperty("size", 0.0);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * If you're using PaperInputBehavior to implement your own paper-input-like
     * element, bind this to the {@code <input is="iron-input">}'s {@code size}
     * property.
     * </p>
     * 
     * @param size
     *            the double value to set
     */
    protected void setSize(double size) {
        getElement().setProperty("size", size);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * If you're using PaperInputBehavior to implement your own paper-input-like
     * element, bind this to the {@code <input is="iron-input">}'s
     * {@code autocapitalize} property.
     * <p>
     * This property is not synchronized automatically from the client side, so
     * the returned value may not be the same as in client side.
     * </p>
     * 
     * @return the {@code autocapitalize} property from the webcomponent
     */
    protected String getAutocapitalizeString() {
        return getElement().getProperty("autocapitalize");
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * If you're using PaperInputBehavior to implement your own paper-input-like
     * element, bind this to the {@code <input is="iron-input">}'s
     * {@code autocapitalize} property.
     * </p>
     * 
     * @param autocapitalize
     *            the String value to set
     */
    protected void setAutocapitalize(String autocapitalize) {
        getElement().setProperty("autocapitalize",
                autocapitalize == null ? "" : autocapitalize);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * If you're using PaperInputBehavior to implement your own paper-input-like
     * element, bind this to the {@code <input is="iron-input">}'s
     * {@code autocorrect} property.
     * <p>
     * This property is not synchronized automatically from the client side, so
     * the returned value may not be the same as in client side.
     * </p>
     * 
     * @return the {@code autocorrect} property from the webcomponent
     */
    protected String getAutocorrectString() {
        return getElement().getProperty("autocorrect");
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * If you're using PaperInputBehavior to implement your own paper-input-like
     * element, bind this to the {@code <input is="iron-input">}'s
     * {@code autocorrect} property.
     * </p>
     * 
     * @param autocorrect
     *            the String value to set
     */
    protected void setAutocorrect(String autocorrect) {
        getElement().setProperty("autocorrect",
                autocorrect == null ? "" : autocorrect);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * If you're using PaperInputBehavior to implement your own paper-input-like
     * element, bind this to the {@code <input is="iron-input">}'s
     * {@code autosave} property, used with type=search.
     * <p>
     * This property is not synchronized automatically from the client side, so
     * the returned value may not be the same as in client side.
     * </p>
     * 
     * @return the {@code autosave} property from the webcomponent
     */
    protected String getAutosaveString() {
        return getElement().getProperty("autosave");
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * If you're using PaperInputBehavior to implement your own paper-input-like
     * element, bind this to the {@code <input is="iron-input">}'s
     * {@code autosave} property, used with type=search.
     * </p>
     * 
     * @param autosave
     *            the String value to set
     */
    protected void setAutosave(String autosave) {
        getElement().setProperty("autosave", autosave == null ? "" : autosave);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * If you're using PaperInputBehavior to implement your own paper-input-like
     * element, bind this to the {@code <input is="iron-input">}'s
     * {@code results} property, used with type=search.
     * <p>
     * This property is not synchronized automatically from the client side, so
     * the returned value may not be the same as in client side.
     * </p>
     * 
     * @return the {@code results} property from the webcomponent
     */
    protected double getResultsDouble() {
        return getElement().getProperty("results", 0.0);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * If you're using PaperInputBehavior to implement your own paper-input-like
     * element, bind this to the {@code <input is="iron-input">}'s
     * {@code results} property, used with type=search.
     * </p>
     * 
     * @param results
     *            the double value to set
     */
    protected void setResults(double results) {
        getElement().setProperty("results", results);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * If you're using PaperInputBehavior to implement your own paper-input-like
     * element, bind this to the {@code <input is="iron-input">}'s
     * {@code accept} property, used with type=file.
     * <p>
     * This property is not synchronized automatically from the client side, so
     * the returned value may not be the same as in client side.
     * </p>
     * 
     * @return the {@code accept} property from the webcomponent
     */
    protected String getAcceptString() {
        return getElement().getProperty("accept");
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * If you're using PaperInputBehavior to implement your own paper-input-like
     * element, bind this to the {@code <input is="iron-input">}'s
     * {@code accept} property, used with type=file.
     * </p>
     * 
     * @param accept
     *            the String value to set
     */
    protected void setAccept(String accept) {
        getElement().setProperty("accept", accept == null ? "" : accept);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * If you're using PaperInputBehavior to implement your own paper-input-like
     * element, bind this to the{@code <input is="iron-input">}'s
     * {@code multiple} property, used with type=file.
     * <p>
     * This property is not synchronized automatically from the client side, so
     * the returned value may not be the same as in client side.
     * </p>
     * 
     * @return the {@code multiple} property from the webcomponent
     */
    protected boolean isMultipleBoolean() {
        return getElement().getProperty("multiple", false);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * If you're using PaperInputBehavior to implement your own paper-input-like
     * element, bind this to the{@code <input is="iron-input">}'s
     * {@code multiple} property, used with type=file.
     * </p>
     * 
     * @param multiple
     *            the boolean value to set
     */
    protected void setMultiple(boolean multiple) {
        getElement().setProperty("multiple", multiple);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * The initial number of rows.
     * <p>
     * This property is not synchronized automatically from the client side, so
     * the returned value may not be the same as in client side.
     * </p>
     * 
     * @return the {@code rows} property from the webcomponent
     */
    protected double getRowsDouble() {
        return getElement().getProperty("rows", 0.0);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * The initial number of rows.
     * </p>
     * 
     * @param rows
     *            the double value to set
     */
    protected void setRows(double rows) {
        getElement().setProperty("rows", rows);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * The maximum number of rows this element can grow to until it scrolls. 0
     * means no maximum.
     * <p>
     * This property is not synchronized automatically from the client side, so
     * the returned value may not be the same as in client side.
     * </p>
     * 
     * @return the {@code maxRows} property from the webcomponent
     */
    protected double getMaxRowsDouble() {
        return getElement().getProperty("maxRows", 0.0);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * The maximum number of rows this element can grow to until it scrolls. 0
     * means no maximum.
     * </p>
     * 
     * @param maxRows
     *            the double value to set
     */
    protected void setMaxRows(double maxRows) {
        getElement().setProperty("maxRows", maxRows);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * Can be used to imperatively add a key binding to the implementing
     * element. This is the imperative equivalent of declaring a keybinding in
     * the {@code keyBindings} prototype property.
     * </p>
     * 
     * @param eventString
     *            Missing documentation!
     * @param handlerName
     *            Missing documentation!
     */
    protected void addOwnKeyBinding(String eventString, String handlerName) {
        getElement().callFunction("addOwnKeyBinding", eventString, handlerName);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * When called, will remove all imperatively-added key bindings.
     * </p>
     */
    protected void removeOwnKeyBindings() {
        getElement().callFunction("removeOwnKeyBindings");
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * Returns true if a keyboard event matches {@code eventString}.
     * </p>
     * <p>
     * This function is not supported by Flow because it returns a
     * <code>boolean</code>. Functions with return types different than void are
     * not supported at this moment.
     * 
     * @param event
     *            Missing documentation!
     * @param eventString
     *            Missing documentation!
     */
    @NotSupported
    protected void keyboardEventMatchesKeys(JsonObject event,
            String eventString) {
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * Returns a reference to the input element.
     * </p>
     */
    protected void inputElement() {
        getElement().callFunction("inputElement");
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * Validates the input element and sets an error style if needed.
     * </p>
     * <p>
     * This function is not supported by Flow because it returns a
     * <code>boolean</code>. Functions with return types different than void are
     * not supported at this moment.
     */
    @NotSupported
    protected void validate() {
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * Restores the cursor to its original position after updating the value.
     * </p>
     * 
     * @param newValue
     *            The value that should be saved.
     */
    protected void updateValueAndPreserveCaret(String newValue) {
        getElement().callFunction("updateValueAndPreserveCaret", newValue);
    }

    @DomEvent("focused-changed")
    public static class FocusedChangeEvent<R extends GeneratedPaperTextarea<R>>
            extends ComponentEvent<R> {
        public FocusedChangeEvent(R source, boolean fromClient) {
            super(source, fromClient);
        }
    }

    /**
     * Adds a listener for {@code focused-changed} events fired by the
     * webcomponent.
     * 
     * @param listener
     *            the listener
     * @return a {@link Registration} for removing the event listener
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    protected Registration addFocusedChangeListener(
            ComponentEventListener<FocusedChangeEvent<R>> listener) {
        return addListener(FocusedChangeEvent.class,
                (ComponentEventListener) listener);
    }

    @DomEvent("disabled-changed")
    public static class DisabledChangeEvent<R extends GeneratedPaperTextarea<R>>
            extends ComponentEvent<R> {
        public DisabledChangeEvent(R source, boolean fromClient) {
            super(source, fromClient);
        }
    }

    /**
     * Adds a listener for {@code disabled-changed} events fired by the
     * webcomponent.
     * 
     * @param listener
     *            the listener
     * @return a {@link Registration} for removing the event listener
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    protected Registration addDisabledChangeListener(
            ComponentEventListener<DisabledChangeEvent<R>> listener) {
        return addListener(DisabledChangeEvent.class,
                (ComponentEventListener) listener);
    }

    @DomEvent("change")
    public static class ChangeEvent<R extends GeneratedPaperTextarea<R>>
            extends ComponentEvent<R> {
        public ChangeEvent(R source, boolean fromClient) {
            super(source, fromClient);
        }
    }

    /**
     * Adds a listener for {@code change} events fired by the webcomponent.
     * 
     * @param listener
     *            the listener
     * @return a {@link Registration} for removing the event listener
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    protected Registration addChangeListener(
            ComponentEventListener<ChangeEvent<R>> listener) {
        return addListener(ChangeEvent.class,
                (ComponentEventListener) listener);
    }

    @DomEvent("value-changed")
    public static class ValueChangeEvent<R extends GeneratedPaperTextarea<R>>
            extends ComponentEvent<R> {
        public ValueChangeEvent(R source, boolean fromClient) {
            super(source, fromClient);
        }
    }

    /**
     * Adds a listener for {@code value-changed} events fired by the
     * webcomponent.
     * 
     * @param listener
     *            the listener
     * @return a {@link Registration} for removing the event listener
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    protected Registration addValueChangeListener(
            ComponentEventListener<ValueChangeEvent<R>> listener) {
        return addListener(ValueChangeEvent.class,
                (ComponentEventListener) listener);
    }

    @DomEvent("invalid-changed")
    public static class InvalidChangeEvent<R extends GeneratedPaperTextarea<R>>
            extends ComponentEvent<R> {
        public InvalidChangeEvent(R source, boolean fromClient) {
            super(source, fromClient);
        }
    }

    /**
     * Adds a listener for {@code invalid-changed} events fired by the
     * webcomponent.
     * 
     * @param listener
     *            the listener
     * @return a {@link Registration} for removing the event listener
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    protected Registration addInvalidChangeListener(
            ComponentEventListener<InvalidChangeEvent<R>> listener) {
        return addListener(InvalidChangeEvent.class,
                (ComponentEventListener) listener);
    }

    @DomEvent("iron-form-element-register")
    public static class IronFormElementRegisterEvent<R extends GeneratedPaperTextarea<R>>
            extends ComponentEvent<R> {
        public IronFormElementRegisterEvent(R source, boolean fromClient) {
            super(source, fromClient);
        }
    }

    /**
     * Adds a listener for {@code iron-form-element-register} events fired by
     * the webcomponent.
     * 
     * @param listener
     *            the listener
     * @return a {@link Registration} for removing the event listener
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    protected Registration addIronFormElementRegisterListener(
            ComponentEventListener<IronFormElementRegisterEvent<R>> listener) {
        return addListener(IronFormElementRegisterEvent.class,
                (ComponentEventListener) listener);
    }

    @DomEvent("iron-form-element-unregister")
    public static class IronFormElementUnregisterEvent<R extends GeneratedPaperTextarea<R>>
            extends ComponentEvent<R> {
        public IronFormElementUnregisterEvent(R source, boolean fromClient) {
            super(source, fromClient);
        }
    }

    /**
     * Adds a listener for {@code iron-form-element-unregister} events fired by
     * the webcomponent.
     * 
     * @param listener
     *            the listener
     * @return a {@link Registration} for removing the event listener
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    protected Registration addIronFormElementUnregisterListener(
            ComponentEventListener<IronFormElementUnregisterEvent<R>> listener) {
        return addListener(IronFormElementUnregisterEvent.class,
                (ComponentEventListener) listener);
    }
}