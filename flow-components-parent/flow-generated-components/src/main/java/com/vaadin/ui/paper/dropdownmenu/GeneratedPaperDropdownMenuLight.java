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
package com.vaadin.ui.paper.dropdownmenu;

import com.vaadin.ui.Component;
import com.vaadin.ui.common.HasStyle;
import com.vaadin.ui.common.ComponentSupplier;
import javax.annotation.Generated;
import com.vaadin.ui.Tag;
import com.vaadin.ui.common.HtmlImport;
import elemental.json.JsonObject;
import com.vaadin.ui.event.Synchronize;
import com.vaadin.ui.common.NotSupported;
import com.vaadin.ui.event.DomEvent;
import com.vaadin.ui.event.ComponentEvent;
import com.vaadin.ui.event.ComponentEventListener;
import com.vaadin.shared.Registration;

@Generated({ "Generator: com.vaadin.generator.ComponentGenerator#1.0-SNAPSHOT",
        "WebComponent: paper-dropdown-menu-light#2.0.0", "Flow#1.0-SNAPSHOT" })
@Tag("paper-dropdown-menu-light")
@HtmlImport("frontend://bower_components/paper-dropdown-menu/paper-dropdown-menu-light.html")
public class GeneratedPaperDropdownMenuLight<R extends GeneratedPaperDropdownMenuLight<R>>
        extends Component implements HasStyle, ComponentSupplier<R> {

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
    protected JsonObject protectedGetKeyEventTarget() {
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
    public boolean isStopKeyboardEventPropagation() {
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
    public void setStopKeyboardEventPropagation(
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
    protected JsonObject protectedGetKeyBindings() {
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
     * If true, the user is currently holding down the button.
     * <p>
     * This property is not synchronized automatically from the client side, so
     * the returned value may not be the same as in client side.
     * </p>
     * 
     * @return the {@code pressed} property from the webcomponent
     */
    public boolean isPressed() {
        return getElement().getProperty("pressed", false);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * If true, the button toggles the active state with each tap or press of
     * the spacebar.
     * <p>
     * This property is not synchronized automatically from the client side, so
     * the returned value may not be the same as in client side.
     * </p>
     * 
     * @return the {@code toggles} property from the webcomponent
     */
    public boolean isToggles() {
        return getElement().getProperty("toggles", false);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * If true, the button toggles the active state with each tap or press of
     * the spacebar.
     * </p>
     * 
     * @param toggles
     *            the boolean value to set
     */
    public void setToggles(boolean toggles) {
        getElement().setProperty("toggles", toggles);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * If true, the button is a toggle and is currently in the active state.
     * <p>
     * This property is synchronized automatically from client side when a
     * 'active-changed' event happens.
     * </p>
     * 
     * @return the {@code active} property from the webcomponent
     */
    @Synchronize(property = "active", value = "active-changed")
    public boolean isActive() {
        return getElement().getProperty("active", false);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * If true, the button is a toggle and is currently in the active state.
     * </p>
     * 
     * @param active
     *            the boolean value to set
     */
    public void setActive(boolean active) {
        getElement().setProperty("active", active);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * True if the element is currently being pressed by a &quot;pointer,&quot;
     * which is loosely defined as mouse or touch input (but specifically
     * excluding keyboard input).
     * <p>
     * This property is not synchronized automatically from the client side, so
     * the returned value may not be the same as in client side.
     * </p>
     * 
     * @return the {@code pointerDown} property from the webcomponent
     */
    public boolean isPointerDown() {
        return getElement().getProperty("pointerDown", false);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * True if the input device that caused the element to receive focus was a
     * keyboard.
     * <p>
     * This property is not synchronized automatically from the client side, so
     * the returned value may not be the same as in client side.
     * </p>
     * 
     * @return the {@code receivedFocusFromKeyboard} property from the
     *         webcomponent
     */
    public boolean isReceivedFocusFromKeyboard() {
        return getElement().getProperty("receivedFocusFromKeyboard", false);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * The aria attribute to be set if the button is a toggle and in the active
     * state.
     * <p>
     * This property is not synchronized automatically from the client side, so
     * the returned value may not be the same as in client side.
     * </p>
     * 
     * @return the {@code ariaActiveAttribute} property from the webcomponent
     */
    public String getAriaActiveAttribute() {
        return getElement().getProperty("ariaActiveAttribute");
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * The aria attribute to be set if the button is a toggle and in the active
     * state.
     * </p>
     * 
     * @param ariaActiveAttribute
     *            the String value to set
     */
    public void setAriaActiveAttribute(String ariaActiveAttribute) {
        getElement().setProperty("ariaActiveAttribute",
                ariaActiveAttribute == null ? "" : ariaActiveAttribute);
    }

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
    public boolean isFocused() {
        return getElement().getProperty("focused", false);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * If true, the user cannot interact with this element.
     * <p>
     * This property is synchronized automatically from client side when a
     * 'disabled-changed' event happens.
     * </p>
     * 
     * @return the {@code disabled} property from the webcomponent
     */
    @Synchronize(property = "disabled", value = "disabled-changed")
    public boolean isDisabled() {
        return getElement().getProperty("disabled", false);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * If true, the user cannot interact with this element.
     * </p>
     * 
     * @param disabled
     *            the boolean value to set
     */
    public void setDisabled(boolean disabled) {
        getElement().setProperty("disabled", disabled);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * If true, the element will not produce a ripple effect when interacted
     * with via the pointer.
     * <p>
     * This property is not synchronized automatically from the client side, so
     * the returned value may not be the same as in client side.
     * </p>
     * 
     * @return the {@code noink} property from the webcomponent
     */
    public boolean isNoink() {
        return getElement().getProperty("noink", false);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * If true, the element will not produce a ripple effect when interacted
     * with via the pointer.
     * </p>
     * 
     * @param noink
     *            the boolean value to set
     */
    public void setNoink(boolean noink) {
        getElement().setProperty("noink", noink);
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
    public String getName() {
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
    public void setName(String name) {
        getElement().setProperty("name", name == null ? "" : name);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * The value for this element that will be used when submitting in a form.
     * It is read only, and will always have the same value as
     * {@code selectedItemLabel}.
     * <p>
     * This property is synchronized automatically from client side when a
     * 'value-changed' event happens.
     * </p>
     * 
     * @return the {@code value} property from the webcomponent
     */
    @Synchronize(property = "value", value = "value-changed")
    public String getValue() {
        return getElement().getProperty("value");
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
    public boolean isRequired() {
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
    public void setRequired(boolean required) {
        getElement().setProperty("required", required);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * Name of the validator to use.
     * <p>
     * This property is not synchronized automatically from the client side, so
     * the returned value may not be the same as in client side.
     * </p>
     * 
     * @return the {@code validator} property from the webcomponent
     */
    public String getValidator() {
        return getElement().getProperty("validator");
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * Name of the validator to use.
     * </p>
     * 
     * @param validator
     *            the String value to set
     */
    public void setValidator(String validator) {
        getElement().setProperty("validator",
                validator == null ? "" : validator);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * True if the last call to {@code validate} is invalid.
     * <p>
     * This property is synchronized automatically from client side when a
     * 'invalid-changed' event happens.
     * </p>
     * 
     * @return the {@code invalid} property from the webcomponent
     */
    @Synchronize(property = "invalid", value = "invalid-changed")
    public boolean isInvalid() {
        return getElement().getProperty("invalid", false);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * True if the last call to {@code validate} is invalid.
     * </p>
     * 
     * @param invalid
     *            the boolean value to set
     */
    public void setInvalid(boolean invalid) {
        getElement().setProperty("invalid", invalid);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * The derived &quot;label&quot; of the currently selected item. This value
     * is the {@code label} property on the selected item if set, or else the
     * trimmed text content of the selected item.
     * <p>
     * This property is not synchronized automatically from the client side, so
     * the returned value may not be the same as in client side.
     * </p>
     * 
     * @return the {@code selectedItemLabel} property from the webcomponent
     */
    public String getSelectedItemLabel() {
        return getElement().getProperty("selectedItemLabel");
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * The last selected item. An item is selected if the dropdown menu has a
     * child with class {@code dropdown-content}, and that child triggers an
     * {@code iron-select} event with the selected {@code item} in the
     * {@code detail}.
     * <p>
     * This property is not synchronized automatically from the client side, so
     * the returned value may not be the same as in client side.
     * </p>
     * 
     * @return the {@code selectedItem} property from the webcomponent
     */
    protected JsonObject protectedGetSelectedItem() {
        return (JsonObject) getElement().getPropertyRaw("selectedItem");
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * The label for the dropdown.
     * <p>
     * This property is not synchronized automatically from the client side, so
     * the returned value may not be the same as in client side.
     * </p>
     * 
     * @return the {@code label} property from the webcomponent
     */
    public String getLabel() {
        return getElement().getProperty("label");
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * The label for the dropdown.
     * </p>
     * 
     * @param label
     *            the String value to set
     */
    public void setLabel(String label) {
        getElement().setProperty("label", label == null ? "" : label);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * The placeholder for the dropdown.
     * <p>
     * This property is not synchronized automatically from the client side, so
     * the returned value may not be the same as in client side.
     * </p>
     * 
     * @return the {@code placeholder} property from the webcomponent
     */
    public String getPlaceholder() {
        return getElement().getProperty("placeholder");
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * The placeholder for the dropdown.
     * </p>
     * 
     * @param placeholder
     *            the String value to set
     */
    public void setPlaceholder(String placeholder) {
        getElement().setProperty("placeholder",
                placeholder == null ? "" : placeholder);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * True if the dropdown is open. Otherwise, false.
     * <p>
     * This property is synchronized automatically from client side when a
     * 'opened-changed' event happens.
     * </p>
     * 
     * @return the {@code opened} property from the webcomponent
     */
    @Synchronize(property = "opened", value = "opened-changed")
    public boolean isOpened() {
        return getElement().getProperty("opened", false);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * True if the dropdown is open. Otherwise, false.
     * </p>
     * 
     * @param opened
     *            the boolean value to set
     */
    public void setOpened(boolean opened) {
        getElement().setProperty("opened", opened);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * By default, the dropdown will constrain scrolling on the page to itself
     * when opened. Set to true in order to prevent scroll from being
     * constrained to the dropdown when it opens.
     * <p>
     * This property is not synchronized automatically from the client side, so
     * the returned value may not be the same as in client side.
     * </p>
     * 
     * @return the {@code allowOutsideScroll} property from the webcomponent
     */
    public boolean isAllowOutsideScroll() {
        return getElement().getProperty("allowOutsideScroll", false);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * By default, the dropdown will constrain scrolling on the page to itself
     * when opened. Set to true in order to prevent scroll from being
     * constrained to the dropdown when it opens.
     * </p>
     * 
     * @param allowOutsideScroll
     *            the boolean value to set
     */
    public void setAllowOutsideScroll(boolean allowOutsideScroll) {
        getElement().setProperty("allowOutsideScroll", allowOutsideScroll);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * Set to true to disable the floating label. Bind this to the
     * {@code <paper-input-container>}'s {@code noLabelFloat} property.
     * <p>
     * This property is not synchronized automatically from the client side, so
     * the returned value may not be the same as in client side.
     * </p>
     * 
     * @return the {@code noLabelFloat} property from the webcomponent
     */
    public boolean isNoLabelFloat() {
        return getElement().getProperty("noLabelFloat", false);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * Set to true to disable the floating label. Bind this to the
     * {@code <paper-input-container>}'s {@code noLabelFloat} property.
     * </p>
     * 
     * @param noLabelFloat
     *            the boolean value to set
     */
    public void setNoLabelFloat(boolean noLabelFloat) {
        getElement().setProperty("noLabelFloat", noLabelFloat);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * Set to true to always float the label. Bind this to the
     * {@code <paper-input-container>}'s {@code alwaysFloatLabel} property.
     * <p>
     * This property is not synchronized automatically from the client side, so
     * the returned value may not be the same as in client side.
     * </p>
     * 
     * @return the {@code alwaysFloatLabel} property from the webcomponent
     */
    public boolean isAlwaysFloatLabel() {
        return getElement().getProperty("alwaysFloatLabel", false);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * Set to true to always float the label. Bind this to the
     * {@code <paper-input-container>}'s {@code alwaysFloatLabel} property.
     * </p>
     * 
     * @param alwaysFloatLabel
     *            the boolean value to set
     */
    public void setAlwaysFloatLabel(boolean alwaysFloatLabel) {
        getElement().setProperty("alwaysFloatLabel", alwaysFloatLabel);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * Set to true to disable animations when opening and closing the dropdown.
     * <p>
     * This property is not synchronized automatically from the client side, so
     * the returned value may not be the same as in client side.
     * </p>
     * 
     * @return the {@code noAnimations} property from the webcomponent
     */
    public boolean isNoAnimations() {
        return getElement().getProperty("noAnimations", false);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * Set to true to disable animations when opening and closing the dropdown.
     * </p>
     * 
     * @param noAnimations
     *            the boolean value to set
     */
    public void setNoAnimations(boolean noAnimations) {
        getElement().setProperty("noAnimations", noAnimations);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * The orientation against which to align the menu dropdown horizontally
     * relative to the dropdown trigger.
     * <p>
     * This property is not synchronized automatically from the client side, so
     * the returned value may not be the same as in client side.
     * </p>
     * 
     * @return the {@code horizontalAlign} property from the webcomponent
     */
    public String getHorizontalAlign() {
        return getElement().getProperty("horizontalAlign");
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * The orientation against which to align the menu dropdown horizontally
     * relative to the dropdown trigger.
     * </p>
     * 
     * @param horizontalAlign
     *            the String value to set
     */
    public void setHorizontalAlign(String horizontalAlign) {
        getElement().setProperty("horizontalAlign",
                horizontalAlign == null ? "" : horizontalAlign);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * The orientation against which to align the menu dropdown vertically
     * relative to the dropdown trigger.
     * <p>
     * This property is not synchronized automatically from the client side, so
     * the returned value may not be the same as in client side.
     * </p>
     * 
     * @return the {@code verticalAlign} property from the webcomponent
     */
    public String getVerticalAlign() {
        return getElement().getProperty("verticalAlign");
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * The orientation against which to align the menu dropdown vertically
     * relative to the dropdown trigger.
     * </p>
     * 
     * @param verticalAlign
     *            the String value to set
     */
    public void setVerticalAlign(String verticalAlign) {
        getElement().setProperty("verticalAlign",
                verticalAlign == null ? "" : verticalAlign);
    }

    /**
     * This property is not synchronized automatically from the client side, so
     * the returned value may not be the same as in client side.
     * 
     * @return the {@code hasContent} property from the webcomponent
     */
    public boolean hasContent() {
        return getElement().getProperty("hasContent", false);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * The content element that is contained by the dropdown menu, if any.
     * <p>
     * This property is not synchronized automatically from the client side, so
     * the returned value may not be the same as in client side.
     * </p>
     * 
     * @return the {@code contentElement} property from the webcomponent
     */
    protected JsonObject protectedGetContentElement() {
        return (JsonObject) getElement().getPropertyRaw("contentElement");
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * The content element that is contained by the dropdown menu, if any.
     * </p>
     * 
     * @param contentElement
     *            the JsonObject value to set
     */
    protected void setContentElement(JsonObject contentElement) {
        getElement().setPropertyJson("contentElement", contentElement);
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
    public void addOwnKeyBinding(String eventString, String handlerName) {
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
    public void removeOwnKeyBindings() {
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
     * Ensures this element contains a ripple effect. For startup efficiency the
     * ripple effect is dynamically on demand when needed.
     * </p>
     * 
     * @param optTriggeringEvent
     *            (optional) event that triggered the ripple.
     */
    protected void ensureRipple(JsonObject optTriggeringEvent) {
        getElement().callFunction("ensureRipple", optTriggeringEvent);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * Returns the {@code <paper-ripple>} element used by this element to create
     * ripple effects. The element's ripple is created on demand, when
     * necessary, and calling this method will force the ripple to be created.
     * </p>
     */
    public void getRipple() {
        getElement().callFunction("getRipple");
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * Returns true if this element currently contains a ripple effect.
     * </p>
     * <p>
     * This function is not supported by Flow because it returns a
     * <code>boolean</code>. Functions with return types different than void are
     * not supported at this moment.
     */
    @NotSupported
    protected void hasRipple() {
    }

    /**
     * <p>
     * This function is not supported by Flow because it returns a
     * <code>boolean</code>. Functions with return types different than void are
     * not supported at this moment.
     */
    @NotSupported
    protected void hasValidator() {
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * Returns true if the {@code value} is valid, and updates {@code invalid}.
     * If you want your element to have custom validation logic, do not override
     * this method; override {@code _getValidity(value)} instead.
     * </p>
     * <p>
     * This function is not supported by Flow because it returns a
     * <code>boolean</code>. Functions with return types different than void are
     * not supported at this moment.
     * 
     * @param value
     *            Deprecated: The value to be validated. By default, it is
     *            passed to the validator's `validate()` function, if a
     *            validator is set. If this argument is not specified, then the
     *            element's `value` property is used, if it exists.
     */
    @NotSupported
    protected void validate(JsonObject value) {
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * Show the dropdown content.
     * </p>
     */
    public void open() {
        getElement().callFunction("open");
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * Hide the dropdown content.
     * </p>
     */
    public void close() {
        getElement().callFunction("close");
    }

    @DomEvent("active-changed")
    public static class ActiveChangeEvent<R extends GeneratedPaperDropdownMenuLight<R>>
            extends ComponentEvent<R> {
        public ActiveChangeEvent(R source, boolean fromClient) {
            super(source, fromClient);
        }
    }

    /**
     * Adds a listener for {@code active-changed} events fired by the
     * webcomponent.
     * 
     * @param listener
     *            the listener
     */
    public Registration addActiveChangeListener(
            ComponentEventListener<ActiveChangeEvent<R>> listener) {
        return addListener(ActiveChangeEvent.class,
                (ComponentEventListener) listener);
    }

    @DomEvent("focused-changed")
    public static class FocusedChangeEvent<R extends GeneratedPaperDropdownMenuLight<R>>
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
     */
    public Registration addFocusedChangeListener(
            ComponentEventListener<FocusedChangeEvent<R>> listener) {
        return addListener(FocusedChangeEvent.class,
                (ComponentEventListener) listener);
    }

    @DomEvent("disabled-changed")
    public static class DisabledChangeEvent<R extends GeneratedPaperDropdownMenuLight<R>>
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
     */
    public Registration addDisabledChangeListener(
            ComponentEventListener<DisabledChangeEvent<R>> listener) {
        return addListener(DisabledChangeEvent.class,
                (ComponentEventListener) listener);
    }

    @DomEvent("iron-form-element-register")
    public static class IronFormElementRegisterEvent<R extends GeneratedPaperDropdownMenuLight<R>>
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
     */
    public Registration addIronFormElementRegisterListener(
            ComponentEventListener<IronFormElementRegisterEvent<R>> listener) {
        return addListener(IronFormElementRegisterEvent.class,
                (ComponentEventListener) listener);
    }

    @DomEvent("iron-form-element-unregister")
    public static class IronFormElementUnregisterEvent<R extends GeneratedPaperDropdownMenuLight<R>>
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
     */
    public Registration addIronFormElementUnregisterListener(
            ComponentEventListener<IronFormElementUnregisterEvent<R>> listener) {
        return addListener(IronFormElementUnregisterEvent.class,
                (ComponentEventListener) listener);
    }

    @DomEvent("value-changed")
    public static class ValueChangeEvent<R extends GeneratedPaperDropdownMenuLight<R>>
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
     */
    public Registration addValueChangeListener(
            ComponentEventListener<ValueChangeEvent<R>> listener) {
        return addListener(ValueChangeEvent.class,
                (ComponentEventListener) listener);
    }

    @DomEvent("invalid-changed")
    public static class InvalidChangeEvent<R extends GeneratedPaperDropdownMenuLight<R>>
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
     */
    public Registration addInvalidChangeListener(
            ComponentEventListener<InvalidChangeEvent<R>> listener) {
        return addListener(InvalidChangeEvent.class,
                (ComponentEventListener) listener);
    }

    @DomEvent("selected-item-label-changed")
    public static class SelectedItemLabelChangeEvent<R extends GeneratedPaperDropdownMenuLight<R>>
            extends ComponentEvent<R> {
        public SelectedItemLabelChangeEvent(R source, boolean fromClient) {
            super(source, fromClient);
        }
    }

    /**
     * Adds a listener for {@code selected-item-label-changed} events fired by
     * the webcomponent.
     * 
     * @param listener
     *            the listener
     */
    public Registration addSelectedItemLabelChangeListener(
            ComponentEventListener<SelectedItemLabelChangeEvent<R>> listener) {
        return addListener(SelectedItemLabelChangeEvent.class,
                (ComponentEventListener) listener);
    }

    @DomEvent("selected-item-changed")
    public static class SelectedItemChangeEvent<R extends GeneratedPaperDropdownMenuLight<R>>
            extends ComponentEvent<R> {
        public SelectedItemChangeEvent(R source, boolean fromClient) {
            super(source, fromClient);
        }
    }

    /**
     * Adds a listener for {@code selected-item-changed} events fired by the
     * webcomponent.
     * 
     * @param listener
     *            the listener
     */
    public Registration addSelectedItemChangeListener(
            ComponentEventListener<SelectedItemChangeEvent<R>> listener) {
        return addListener(SelectedItemChangeEvent.class,
                (ComponentEventListener) listener);
    }

    @DomEvent("opened-changed")
    public static class OpenedChangeEvent<R extends GeneratedPaperDropdownMenuLight<R>>
            extends ComponentEvent<R> {
        public OpenedChangeEvent(R source, boolean fromClient) {
            super(source, fromClient);
        }
    }

    /**
     * Adds a listener for {@code opened-changed} events fired by the
     * webcomponent.
     * 
     * @param listener
     *            the listener
     */
    public Registration addOpenedChangeListener(
            ComponentEventListener<OpenedChangeEvent<R>> listener) {
        return addListener(OpenedChangeEvent.class,
                (ComponentEventListener) listener);
    }

    /**
     * Adds the given components as children of this component at the slot
     * 'dropdown-content'.
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
    public R addToDropdownContent(com.vaadin.ui.Component... components) {
        for (Component component : components) {
            component.getElement().setAttribute("slot", "dropdown-content");
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
        getElement().getChildren()
                .forEach(child -> child.removeAttribute("slot"));
        getElement().removeAllChildren();
    }
}