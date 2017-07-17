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
package com.vaadin.generated.paper.dropdown.menu;

import com.vaadin.ui.Component;
import com.vaadin.ui.HasStyle;
import javax.annotation.Generated;
import com.vaadin.annotations.Tag;
import com.vaadin.annotations.HtmlImport;
import elemental.json.JsonObject;
import com.vaadin.generated.paper.dropdown.menu.GeneratedPaperDropdownMenu;
import com.vaadin.annotations.Synchronize;
import com.vaadin.components.NotSupported;
import com.vaadin.annotations.DomEvent;
import com.vaadin.ui.ComponentEvent;
import com.vaadin.flow.event.ComponentEventListener;
import com.vaadin.shared.Registration;

/**
 * Description copied from corresponding location in WebComponent:
 * 
 * Material design: [Dropdown
 * menus](https://www.google.com/design/spec/components
 * /buttons.html#buttons-dropdown-buttons)
 * 
 * {@code paper-dropdown-menu} is similar to a native browser select element.
 * {@code paper-dropdown-menu} works with selectable content. The currently
 * selected item is displayed in the control. If no item is selected, the
 * {@code label} is displayed instead.
 * 
 * Example:
 * 
 * <paper-dropdown-menu label="Your favourite pastry"> <paper-listbox
 * slot="dropdown-content"> <paper-item>Croissant</paper-item>
 * <paper-item>Donut</paper-item> <paper-item>Financier</paper-item>
 * <paper-item>Madeleine</paper-item> </paper-listbox> </paper-dropdown-menu>
 * 
 * This example renders a dropdown menu with 4 options.
 * 
 * The child element with the slot {@code dropdown-content} is used as the
 * dropdown menu. This can be a [{@code paper-listbox}](paper-listbox), or any
 * other or element that acts like an [{@code iron-selector}](iron-selector).
 * 
 * Specifically, the menu child must fire an [{@code iron-select}
 * ](iron-selector#event-iron-select) event when one of its children is
 * selected, and an [{@code iron-deselect}](iron-selector#event-iron-deselect)
 * event when a child is deselected. The selected or deselected item must be
 * passed as the event's {@code detail.item} property.
 * 
 * Applications can listen for the {@code iron-select} and {@code iron-deselect}
 * events to react when options are selected and deselected.
 * 
 * ### Styling
 * 
 * The following custom properties and mixins are also available for styling:
 * 
 * Custom property | Description | Default
 * ----------------|-------------|---------- {@code --paper-dropdown-menu} | A
 * mixin that is applied to the element host | {@code
 * {@code --paper-dropdown-menu-disabled} | A mixin that is applied to the
 * element host when disabled | {@code {@code --paper-dropdown-menu-ripple} | A
 * mixin that is applied to the internal ripple | {@code
 * {@code --paper-dropdown-menu-button} | A mixin that is applied to the
 * internal menu button | {@code {@code --paper-dropdown-menu-input} | A mixin
 * that is applied to the internal paper input | {@code
 * {@code --paper-dropdown-menu-icon} | A mixin that is applied to the internal
 * icon | {@code
 * 
 * You can also use any of the {@code paper-input-container} and
 * {@code paper-menu-button} style mixins and custom properties to style the
 * internal input and menu button respectively.
 */
@Generated({
		"Generator: com.vaadin.generator.ComponentGenerator#0.1.14-SNAPSHOT",
		"WebComponent: paper-dropdown-menu#2.0.0", "Flow#0.1.14-SNAPSHOT"})
@Tag("paper-dropdown-menu")
@HtmlImport("frontend://bower_components/paper-dropdown-menu/paper-dropdown-menu.html")
public class GeneratedPaperDropdownMenu extends Component implements HasStyle {

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
	 *            the JsonObject value to set
	 * @return this instance, for method chaining
	 */
	public <R extends GeneratedPaperDropdownMenu> R setKeyEventTarget(
			elemental.json.JsonObject keyEventTarget) {
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
	 *            the boolean value to set
	 * @return this instance, for method chaining
	 */
	public <R extends GeneratedPaperDropdownMenu> R setStopKeyboardEventPropagation(
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
	 *            the JsonObject value to set
	 * @return this instance, for method chaining
	 */
	public <R extends GeneratedPaperDropdownMenu> R setKeyBindings(
			elemental.json.JsonObject keyBindings) {
		getElement().setPropertyJson("keyBindings", keyBindings);
		return getSelf();
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * If true, the user is currently holding down the button.
	 * <p>
	 * This property is not synchronized automatically from the client side, so
	 * the returned value may not be the same as in client side.
	 */
	public boolean isPressed() {
		return getElement().getProperty("pressed", false);
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * If true, the button toggles the active state with each tap or press of
	 * the spacebar.
	 * <p>
	 * This property is not synchronized automatically from the client side, so
	 * the returned value may not be the same as in client side.
	 */
	public boolean isToggles() {
		return getElement().getProperty("toggles", false);
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * If true, the button toggles the active state with each tap or press of
	 * the spacebar.
	 * 
	 * @param toggles
	 *            the boolean value to set
	 * @return this instance, for method chaining
	 */
	public <R extends GeneratedPaperDropdownMenu> R setToggles(boolean toggles) {
		getElement().setProperty("toggles", toggles);
		return getSelf();
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * If true, the button is a toggle and is currently in the active state.
	 * <p>
	 * This property is synchronized automatically from client side when a
	 * 'active-changed' event happens.
	 */
	@Synchronize(property = "active", value = "active-changed")
	public boolean isActive() {
		return getElement().getProperty("active", false);
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * If true, the button is a toggle and is currently in the active state.
	 * 
	 * @param active
	 *            the boolean value to set
	 * @return this instance, for method chaining
	 */
	public <R extends GeneratedPaperDropdownMenu> R setActive(boolean active) {
		getElement().setProperty("active", active);
		return getSelf();
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * True if the element is currently being pressed by a "pointer," which is
	 * loosely defined as mouse or touch input (but specifically excluding
	 * keyboard input).
	 * <p>
	 * This property is not synchronized automatically from the client side, so
	 * the returned value may not be the same as in client side.
	 */
	public boolean isPointerDown() {
		return getElement().getProperty("pointerDown", false);
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * True if the input device that caused the element to receive focus was a
	 * keyboard.
	 * <p>
	 * This property is not synchronized automatically from the client side, so
	 * the returned value may not be the same as in client side.
	 */
	public boolean isReceivedFocusFromKeyboard() {
		return getElement().getProperty("receivedFocusFromKeyboard", false);
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * The aria attribute to be set if the button is a toggle and in the active
	 * state.
	 * <p>
	 * This property is not synchronized automatically from the client side, so
	 * the returned value may not be the same as in client side.
	 */
	public String getAriaActiveAttribute() {
		return getElement().getProperty("ariaActiveAttribute");
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * The aria attribute to be set if the button is a toggle and in the active
	 * state.
	 * 
	 * @param ariaActiveAttribute
	 *            the String value to set
	 * @return this instance, for method chaining
	 */
	public <R extends GeneratedPaperDropdownMenu> R setAriaActiveAttribute(
			java.lang.String ariaActiveAttribute) {
		getElement().setProperty("ariaActiveAttribute",
				ariaActiveAttribute == null ? "" : ariaActiveAttribute);
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
	public <R extends GeneratedPaperDropdownMenu> R setDisabled(boolean disabled) {
		getElement().setProperty("disabled", disabled);
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
	 *            the String value to set
	 * @return this instance, for method chaining
	 */
	public <R extends GeneratedPaperDropdownMenu> R setName(
			java.lang.String name) {
		getElement().setProperty("name", name == null ? "" : name);
		return getSelf();
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * The value for this element that will be used when submitting in a form.
	 * It is read only, and will always have the same value as
	 * {@code selectedItemLabel}.
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
	public <R extends GeneratedPaperDropdownMenu> R setRequired(boolean required) {
		getElement().setProperty("required", required);
		return getSelf();
	}

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
	public <R extends GeneratedPaperDropdownMenu> R setValidator(
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
	public <R extends GeneratedPaperDropdownMenu> R setInvalid(boolean invalid) {
		getElement().setProperty("invalid", invalid);
		return getSelf();
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * The derived "label" of the currently selected item. This value is the
	 * {@code label} property on the selected item if set, or else the trimmed
	 * text content of the selected item.
	 * <p>
	 * This property is not synchronized automatically from the client side, so
	 * the returned value may not be the same as in client side.
	 */
	public String getSelectedItemLabel() {
		return getElement().getProperty("selectedItemLabel");
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * The last selected item. An item is selected if the dropdown menu has a
	 * child with slot {@code dropdown-content}, and that child triggers an
	 * {@code iron-select} event with the selected {@code item} in the
	 * {@code detail}.
	 * <p>
	 * This property is not synchronized automatically from the client side, so
	 * the returned value may not be the same as in client side.
	 */
	public JsonObject getSelectedItem() {
		return (JsonObject) getElement().getPropertyRaw("selectedItem");
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * The label for the dropdown.
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
	 * The label for the dropdown.
	 * 
	 * @param label
	 *            the String value to set
	 * @return this instance, for method chaining
	 */
	public <R extends GeneratedPaperDropdownMenu> R setLabel(
			java.lang.String label) {
		getElement().setProperty("label", label == null ? "" : label);
		return getSelf();
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * The placeholder for the dropdown.
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
	 * The placeholder for the dropdown.
	 * 
	 * @param placeholder
	 *            the String value to set
	 * @return this instance, for method chaining
	 */
	public <R extends GeneratedPaperDropdownMenu> R setPlaceholder(
			java.lang.String placeholder) {
		getElement().setProperty("placeholder",
				placeholder == null ? "" : placeholder);
		return getSelf();
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * The error message to display when invalid.
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
	 * The error message to display when invalid.
	 * 
	 * @param errorMessage
	 *            the String value to set
	 * @return this instance, for method chaining
	 */
	public <R extends GeneratedPaperDropdownMenu> R setErrorMessage(
			java.lang.String errorMessage) {
		getElement().setProperty("errorMessage",
				errorMessage == null ? "" : errorMessage);
		return getSelf();
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * True if the dropdown is open. Otherwise, false.
	 * <p>
	 * This property is synchronized automatically from client side when a
	 * 'opened-changed' event happens.
	 */
	@Synchronize(property = "opened", value = "opened-changed")
	public boolean isOpened() {
		return getElement().getProperty("opened", false);
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * True if the dropdown is open. Otherwise, false.
	 * 
	 * @param opened
	 *            the boolean value to set
	 * @return this instance, for method chaining
	 */
	public <R extends GeneratedPaperDropdownMenu> R setOpened(boolean opened) {
		getElement().setProperty("opened", opened);
		return getSelf();
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * By default, the dropdown will constrain scrolling on the page to itself
	 * when opened. Set to true in order to prevent scroll from being
	 * constrained to the dropdown when it opens.
	 * <p>
	 * This property is not synchronized automatically from the client side, so
	 * the returned value may not be the same as in client side.
	 */
	public boolean isAllowOutsideScroll() {
		return getElement().getProperty("allowOutsideScroll", false);
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * By default, the dropdown will constrain scrolling on the page to itself
	 * when opened. Set to true in order to prevent scroll from being
	 * constrained to the dropdown when it opens.
	 * 
	 * @param allowOutsideScroll
	 *            the boolean value to set
	 * @return this instance, for method chaining
	 */
	public <R extends GeneratedPaperDropdownMenu> R setAllowOutsideScroll(
			boolean allowOutsideScroll) {
		getElement().setProperty("allowOutsideScroll", allowOutsideScroll);
		return getSelf();
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Set to true to disable the floating label. Bind this to the
	 * {@code <paper-input-container>}'s {@code noLabelFloat} property.
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
	 * Set to true to disable the floating label. Bind this to the
	 * {@code <paper-input-container>}'s {@code noLabelFloat} property.
	 * 
	 * @param noLabelFloat
	 *            the boolean value to set
	 * @return this instance, for method chaining
	 */
	public <R extends GeneratedPaperDropdownMenu> R setNoLabelFloat(
			boolean noLabelFloat) {
		getElement().setProperty("noLabelFloat", noLabelFloat);
		return getSelf();
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Set to true to always float the label. Bind this to the
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
	 * Set to true to always float the label. Bind this to the
	 * {@code <paper-input-container>}'s {@code alwaysFloatLabel} property.
	 * 
	 * @param alwaysFloatLabel
	 *            the boolean value to set
	 * @return this instance, for method chaining
	 */
	public <R extends GeneratedPaperDropdownMenu> R setAlwaysFloatLabel(
			boolean alwaysFloatLabel) {
		getElement().setProperty("alwaysFloatLabel", alwaysFloatLabel);
		return getSelf();
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Set to true to disable animations when opening and closing the dropdown.
	 * <p>
	 * This property is not synchronized automatically from the client side, so
	 * the returned value may not be the same as in client side.
	 */
	public boolean isNoAnimations() {
		return getElement().getProperty("noAnimations", false);
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Set to true to disable animations when opening and closing the dropdown.
	 * 
	 * @param noAnimations
	 *            the boolean value to set
	 * @return this instance, for method chaining
	 */
	public <R extends GeneratedPaperDropdownMenu> R setNoAnimations(
			boolean noAnimations) {
		getElement().setProperty("noAnimations", noAnimations);
		return getSelf();
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * The orientation against which to align the menu dropdown horizontally
	 * relative to the dropdown trigger.
	 * <p>
	 * This property is not synchronized automatically from the client side, so
	 * the returned value may not be the same as in client side.
	 */
	public String getHorizontalAlign() {
		return getElement().getProperty("horizontalAlign");
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * The orientation against which to align the menu dropdown horizontally
	 * relative to the dropdown trigger.
	 * 
	 * @param horizontalAlign
	 *            the String value to set
	 * @return this instance, for method chaining
	 */
	public <R extends GeneratedPaperDropdownMenu> R setHorizontalAlign(
			java.lang.String horizontalAlign) {
		getElement().setProperty("horizontalAlign",
				horizontalAlign == null ? "" : horizontalAlign);
		return getSelf();
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * The orientation against which to align the menu dropdown vertically
	 * relative to the dropdown trigger.
	 * <p>
	 * This property is not synchronized automatically from the client side, so
	 * the returned value may not be the same as in client side.
	 */
	public String getVerticalAlign() {
		return getElement().getProperty("verticalAlign");
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * The orientation against which to align the menu dropdown vertically
	 * relative to the dropdown trigger.
	 * 
	 * @param verticalAlign
	 *            the String value to set
	 * @return this instance, for method chaining
	 */
	public <R extends GeneratedPaperDropdownMenu> R setVerticalAlign(
			java.lang.String verticalAlign) {
		getElement().setProperty("verticalAlign",
				verticalAlign == null ? "" : verticalAlign);
		return getSelf();
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * If true, the {@code horizontalAlign} and {@code verticalAlign} properties
	 * will be considered preferences instead of strict requirements when
	 * positioning the dropdown and may be changed if doing so reduces the area
	 * of the dropdown falling outside of {@code fitInto}.
	 * <p>
	 * This property is not synchronized automatically from the client side, so
	 * the returned value may not be the same as in client side.
	 */
	public boolean isDynamicAlign() {
		return getElement().getProperty("dynamicAlign", false);
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * If true, the {@code horizontalAlign} and {@code verticalAlign} properties
	 * will be considered preferences instead of strict requirements when
	 * positioning the dropdown and may be changed if doing so reduces the area
	 * of the dropdown falling outside of {@code fitInto}.
	 * 
	 * @param dynamicAlign
	 *            the boolean value to set
	 * @return this instance, for method chaining
	 */
	public <R extends GeneratedPaperDropdownMenu> R setDynamicAlign(
			boolean dynamicAlign) {
		getElement().setProperty("dynamicAlign", dynamicAlign);
		return getSelf();
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Whether focus should be restored to the dropdown when the menu closes.
	 * <p>
	 * This property is not synchronized automatically from the client side, so
	 * the returned value may not be the same as in client side.
	 */
	public boolean isRestoreFocusOnClose() {
		return getElement().getProperty("restoreFocusOnClose", false);
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Whether focus should be restored to the dropdown when the menu closes.
	 * 
	 * @param restoreFocusOnClose
	 *            the boolean value to set
	 * @return this instance, for method chaining
	 */
	public <R extends GeneratedPaperDropdownMenu> R setRestoreFocusOnClose(
			boolean restoreFocusOnClose) {
		getElement().setProperty("restoreFocusOnClose", restoreFocusOnClose);
		return getSelf();
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * The content element that is contained by the dropdown menu, if any.
	 * <p>
	 * This property is not synchronized automatically from the client side, so
	 * the returned value may not be the same as in client side.
	 */
	public JsonObject getContentElement() {
		return (JsonObject) getElement().getPropertyRaw("contentElement");
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * The content element that is contained by the dropdown menu, if any.
	 * 
	 * @param contentElement
	 *            the JsonObject value to set
	 * @return this instance, for method chaining
	 */
	public <R extends GeneratedPaperDropdownMenu> R setContentElement(
			elemental.json.JsonObject contentElement) {
		getElement().setPropertyJson("contentElement", contentElement);
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
	 * @return It would return a boolean
	 */
	@NotSupported
	protected void hasValidator() {
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Returns true if the {@code value} is valid, and updates {@code invalid}.
	 * If you want your element to have custom validation logic, do not override
	 * this method; override {@code _getValidity(value)} instead.
	 * 
	 * @param value
	 *            Deprecated: The value to be validated. By default, it is
	 *            passed to the validator's `validate()` function, if a
	 *            validator is set. If this argument is not specified, then the
	 *            element's `value` property is used, if it exists.
	 * @return It would return a boolean
	 */
	@NotSupported
	protected void validate(elemental.json.JsonObject value) {
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Show the dropdown content.
	 */
	public void open() {
		getElement().callFunction("open");
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Hide the dropdown content.
	 */
	public void close() {
		getElement().callFunction("close");
	}

	@DomEvent("active-changed")
	public static class ActiveChangedEvent
			extends
				ComponentEvent<GeneratedPaperDropdownMenu> {
		public ActiveChangedEvent(GeneratedPaperDropdownMenu source,
				boolean fromClient) {
			super(source, fromClient);
		}
	}

	public Registration addActiveChangedListener(
			ComponentEventListener<ActiveChangedEvent> listener) {
		return addListener(ActiveChangedEvent.class, listener);
	}

	@DomEvent("focused-changed")
	public static class FocusedChangedEvent
			extends
				ComponentEvent<GeneratedPaperDropdownMenu> {
		public FocusedChangedEvent(GeneratedPaperDropdownMenu source,
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
				ComponentEvent<GeneratedPaperDropdownMenu> {
		public DisabledChangedEvent(GeneratedPaperDropdownMenu source,
				boolean fromClient) {
			super(source, fromClient);
		}
	}

	public Registration addDisabledChangedListener(
			ComponentEventListener<DisabledChangedEvent> listener) {
		return addListener(DisabledChangedEvent.class, listener);
	}

	@DomEvent("iron-form-element-register")
	public static class IronFormElementRegisterEvent
			extends
				ComponentEvent<GeneratedPaperDropdownMenu> {
		public IronFormElementRegisterEvent(GeneratedPaperDropdownMenu source,
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
				ComponentEvent<GeneratedPaperDropdownMenu> {
		public IronFormElementUnregisterEvent(
				GeneratedPaperDropdownMenu source, boolean fromClient) {
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
				ComponentEvent<GeneratedPaperDropdownMenu> {
		public ValueChangedEvent(GeneratedPaperDropdownMenu source,
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
				ComponentEvent<GeneratedPaperDropdownMenu> {
		public InvalidChangedEvent(GeneratedPaperDropdownMenu source,
				boolean fromClient) {
			super(source, fromClient);
		}
	}

	public Registration addInvalidChangedListener(
			ComponentEventListener<InvalidChangedEvent> listener) {
		return addListener(InvalidChangedEvent.class, listener);
	}

	@DomEvent("selected-item-label-changed")
	public static class SelectedItemLabelChangedEvent
			extends
				ComponentEvent<GeneratedPaperDropdownMenu> {
		public SelectedItemLabelChangedEvent(GeneratedPaperDropdownMenu source,
				boolean fromClient) {
			super(source, fromClient);
		}
	}

	public Registration addSelectedItemLabelChangedListener(
			ComponentEventListener<SelectedItemLabelChangedEvent> listener) {
		return addListener(SelectedItemLabelChangedEvent.class, listener);
	}

	@DomEvent("selected-item-changed")
	public static class SelectedItemChangedEvent
			extends
				ComponentEvent<GeneratedPaperDropdownMenu> {
		public SelectedItemChangedEvent(GeneratedPaperDropdownMenu source,
				boolean fromClient) {
			super(source, fromClient);
		}
	}

	public Registration addSelectedItemChangedListener(
			ComponentEventListener<SelectedItemChangedEvent> listener) {
		return addListener(SelectedItemChangedEvent.class, listener);
	}

	@DomEvent("opened-changed")
	public static class OpenedChangedEvent
			extends
				ComponentEvent<GeneratedPaperDropdownMenu> {
		public OpenedChangedEvent(GeneratedPaperDropdownMenu source,
				boolean fromClient) {
			super(source, fromClient);
		}
	}

	public Registration addOpenedChangedListener(
			ComponentEventListener<OpenedChangedEvent> listener) {
		return addListener(OpenedChangedEvent.class, listener);
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
	 */
	public void addToDropdownContent(com.vaadin.ui.Component... components) {
		for (Component component : components) {
			component.getElement().setAttribute("slot", "dropdown-content");
			getElement().appendChild(component.getElement());
		}
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

	/**
	 * Gets the narrow typed reference to this object. Subclasses should
	 * override this method to support method chaining using the inherited type.
	 * 
	 * @return This object casted to its type.
	 */
	protected <R extends GeneratedPaperDropdownMenu> R getSelf() {
		return (R) this;
	}
}