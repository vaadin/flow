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
package com.vaadin.components.paper.item;

import com.vaadin.ui.Component;
import com.vaadin.ui.HasStyle;
import javax.annotation.Generated;
import com.vaadin.annotations.Tag;
import com.vaadin.annotations.HtmlImport;
import elemental.json.JsonObject;
import com.vaadin.components.paper.item.PaperItem;
import com.vaadin.annotations.Synchronize;
import com.vaadin.components.NotSupported;
import com.vaadin.annotations.DomEvent;
import com.vaadin.ui.ComponentEvent;
import com.vaadin.flow.event.ComponentEventListener;
import com.vaadin.shared.Registration;
import com.vaadin.ui.HasComponents;

/**
 * Description copied from corresponding location in WebComponent:
 * 
 * Material design:
 * [Lists](https://www.google.com/design/spec/components/lists.html)
 * 
 * {@code <paper-item>} is an interactive list item. By default, it is a
 * horizontal flexbox.
 * 
 * <paper-item>Item</paper-item>
 * 
 * Use this element with {@code <paper-item-body>} to make Material Design
 * styled two-line and three-line items.
 * 
 * <paper-item> <paper-item-body two-line> <div>Show your status</div> <div
 * secondary>Your status is visible to everyone</div> </paper-item-body>
 * <iron-icon icon="warning"></iron-icon> </paper-item>
 * 
 * To use {@code paper-item} as a link, wrap it in an anchor tag. Since
 * {@code paper-item} will already receive focus, you may want to prevent the
 * anchor tag from receiving focus as well by setting its tabindex to -1.
 * 
 * <a href="https://www.polymer-project.org/" tabindex="-1"> <paper-item
 * raised>Polymer Project</paper-item> </a>
 * 
 * If you are concerned about performance and want to use {@code paper-item} in
 * a {@code paper-listbox} with many items, you can just use a native
 * {@code button} with the {@code paper-item} class applied (provided you have
 * correctly included the shared styles):
 * 
 * <style is="custom-style" include="paper-item-shared-styles"></style>
 * 
 * <paper-listbox> <button class="paper-item" role="option">Inbox</button>
 * <button class="paper-item" role="option">Starred</button> <button
 * class="paper-item" role="option">Sent mail</button> </paper-listbox>
 * 
 * ### Styling
 * 
 * The following custom properties and mixins are available for styling:
 * 
 * Custom property | Description | Default
 * ------------------------------|--------
 * --------------------------------------|----------
 * {@code --paper-item-min-height} | Minimum height of the item | {@code 48px}
 * {@code --paper-item} | Mixin applied to the item | {@code
 * {@code --paper-item-selected-weight}| The font weight of a selected item |
 * {@code bold} {@code --paper-item-selected} | Mixin applied to selected
 * paper-items | {@code {@code --paper-item-disabled-color} | The color for
 * disabled paper-items | {@code --disabled-text-color}
 * {@code --paper-item-disabled} | Mixin applied to disabled paper-items |
 * {@code {@code --paper-item-focused} | Mixin applied to focused paper-items |
 * {@code {@code --paper-item-focused-before} | Mixin applied to :before
 * focused paper-items | {@code
 * 
 * ### Accessibility
 * 
 * This element has {@code role="listitem"} by default. Depending on usage, it
 * may be more appropriate to set {@code role="menuitem"},
 * {@code role="menuitemcheckbox"} or {@code role="menuitemradio"}.
 * 
 * <paper-item role="menuitemcheckbox"> <paper-item-body> Show your status
 * </paper-item-body> <paper-checkbox></paper-checkbox> </paper-item>
 */
@Generated({
		"Generator: com.vaadin.generator.ComponentGenerator#0.1.13-SNAPSHOT",
		"WebComponent: paper-item#2.0.0", "Flow#0.1.13-SNAPSHOT"})
@Tag("paper-item")
@HtmlImport("frontend://bower_components/paper-item/paper-item.html")
public class PaperItem extends Component implements HasStyle, HasComponents {

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
	public <R extends PaperItem> R setKeyEventTarget(
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
	public <R extends PaperItem> R setStopKeyboardEventPropagation(
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
	public <R extends PaperItem> R setKeyBindings(
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
	public <R extends PaperItem> R setToggles(boolean toggles) {
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
	public <R extends PaperItem> R setActive(boolean active) {
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
	public <R extends PaperItem> R setAriaActiveAttribute(
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
	public <R extends PaperItem> R setDisabled(boolean disabled) {
		getElement().setProperty("disabled", disabled);
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

	@DomEvent("active-changed")
	public static class ActiveChangedEvent extends ComponentEvent<PaperItem> {
		public ActiveChangedEvent(PaperItem source, boolean fromClient) {
			super(source, fromClient);
		}
	}

	public Registration addActiveChangedListener(
			ComponentEventListener<ActiveChangedEvent> listener) {
		return addListener(ActiveChangedEvent.class, listener);
	}

	@DomEvent("focused-changed")
	public static class FocusedChangedEvent extends ComponentEvent<PaperItem> {
		public FocusedChangedEvent(PaperItem source, boolean fromClient) {
			super(source, fromClient);
		}
	}

	public Registration addFocusedChangedListener(
			ComponentEventListener<FocusedChangedEvent> listener) {
		return addListener(FocusedChangedEvent.class, listener);
	}

	@DomEvent("disabled-changed")
	public static class DisabledChangedEvent extends ComponentEvent<PaperItem> {
		public DisabledChangedEvent(PaperItem source, boolean fromClient) {
			super(source, fromClient);
		}
	}

	public Registration addDisabledChangedListener(
			ComponentEventListener<DisabledChangedEvent> listener) {
		return addListener(DisabledChangedEvent.class, listener);
	}

	/**
	 * Gets the narrow typed reference to this object. Subclasses should
	 * override this method to support method chaining using the inherited type.
	 * 
	 * @return This object casted to its type.
	 */
	protected <R extends PaperItem> R getSelf() {
		return (R) this;
	}

	/**
	 * Adds the given components as children of this component.
	 * 
	 * @param components
	 *            the components to add
	 * @see HasComponents#add(Component...)
	 */
	public PaperItem(com.vaadin.ui.Component... components) {
		add(components);
	}

	/**
	 * Default constructor.
	 */
	public PaperItem() {
	}
}