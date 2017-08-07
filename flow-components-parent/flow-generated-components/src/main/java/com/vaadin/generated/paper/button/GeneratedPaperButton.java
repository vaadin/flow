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
package com.vaadin.generated.paper.button;

import com.vaadin.ui.Component;
import com.vaadin.ui.ComponentSupplier;
import com.vaadin.ui.HasStyle;
import com.vaadin.ui.HasText;
import com.vaadin.ui.Focusable;
import com.vaadin.ui.HasClickListeners;
import javax.annotation.Generated;
import com.vaadin.annotations.Tag;
import com.vaadin.annotations.HtmlImport;
import elemental.json.JsonObject;
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
 * [Buttons](https://www.google.com/design/spec/components/buttons.html)
 * 
 * {@code paper-button} is a button. When the user touches the button, a ripple
 * effect emanates from the point of contact. It may be flat or raised. A raised
 * button is styled with a shadow.
 * 
 * Example:
 * 
 * <paper-button>Flat button</paper-button> <paper-button raised>Raised
 * button</paper-button> <paper-button noink>No ripple effect</paper-button>
 * <paper-button toggles>Toggle-able button</paper-button>
 * 
 * A button that has {@code toggles} true will remain {@code active} after being
 * clicked (and will have an {@code active} attribute set). For more
 * information, see the {@code Polymer.IronButtonState} behavior.
 * 
 * You may use custom DOM in the button body to create a variety of buttons. For
 * example, to create a button with an icon and some text:
 * 
 * <paper-button> <iron-icon icon="favorite"></iron-icon> custom button content
 * </paper-button>
 * 
 * To use {@code paper-button} as a link, wrap it in an anchor tag. Since
 * {@code paper-button} will already receive focus, you may want to prevent the
 * anchor tag from receiving focus as well by setting its tabindex to -1.
 * 
 * <a href="https://www.polymer-project.org/" tabindex="-1"> <paper-button
 * raised>Polymer Project</paper-button> </a>
 * 
 * ### Styling
 * 
 * Style the button with CSS as you would a normal DOM element.
 * 
 * paper-button.fancy { background: green; color: yellow; }
 * 
 * paper-button.fancy:hover { background: lime; }
 * 
 * paper-button[disabled], paper-button[toggles][active] { background: red; }
 * 
 * By default, the ripple is the same color as the foreground at 25% opacity.
 * You may customize the color using the {@code --paper-button-ink-color} custom
 * property.
 * 
 * The following custom properties and mixins are also available for styling:
 * 
 * Custom property | Description | Default
 * ----------------|-------------|---------- {@code --paper-button-ink-color} |
 * Background color of the ripple | {@code Based on the button's color}
 * {@code --paper-button} | Mixin applied to the button | {@code
 * {@code --paper-button-disabled} | Mixin applied to the disabled button. Note
 * that you can also use the {@code paper-button[disabled]} selector | {@code
 * {@code --paper-button-flat-keyboard-focus} | Mixin applied to a flat button
 * after it's been focused using the keyboard | {@code
 * {@code --paper-button-raised-keyboard-focus} | Mixin applied to a raised
 * button after it's been focused using the keyboard | {@code
 */
@Generated({
		"Generator: com.vaadin.generator.ComponentGenerator#0.1.17-SNAPSHOT",
		"WebComponent: paper-button#2.0.0", "Flow#0.1.17-SNAPSHOT"})
@Tag("paper-button")
@HtmlImport("frontend://bower_components/paper-button/paper-button.html")
public class GeneratedPaperButton<R extends GeneratedPaperButton<R>>
		extends
			Component
		implements
			ComponentSupplier<R>,
			HasStyle,
			HasText,
			Focusable<R>,
			HasClickListeners<R>,
			HasComponents {

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
	public R setToggles(boolean toggles) {
		getElement().setProperty("toggles", toggles);
		return get();
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
	public R setActive(boolean active) {
		getElement().setProperty("active", active);
		return get();
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
	public R setAriaActiveAttribute(java.lang.String ariaActiveAttribute) {
		getElement().setProperty("ariaActiveAttribute",
				ariaActiveAttribute == null ? "" : ariaActiveAttribute);
		return get();
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
	public R setDisabled(boolean disabled) {
		getElement().setProperty("disabled", disabled);
		return get();
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * If true, the element will not produce a ripple effect when interacted
	 * with via the pointer.
	 * <p>
	 * This property is not synchronized automatically from the client side, so
	 * the returned value may not be the same as in client side.
	 */
	public boolean isNoink() {
		return getElement().getProperty("noink", false);
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * If true, the element will not produce a ripple effect when interacted
	 * with via the pointer.
	 * 
	 * @param noink
	 *            the boolean value to set
	 * @return this instance, for method chaining
	 */
	public R setNoink(boolean noink) {
		getElement().setProperty("noink", noink);
		return get();
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * The z-depth of this element, from 0-5. Setting to 0 will remove the
	 * shadow, and each increasing number greater than 0 will be "deeper" than
	 * the last.
	 * <p>
	 * This property is not synchronized automatically from the client side, so
	 * the returned value may not be the same as in client side.
	 */
	public double getElevation() {
		return getElement().getProperty("elevation", 0.0);
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * If true, the button should be styled with a shadow.
	 * <p>
	 * This property is not synchronized automatically from the client side, so
	 * the returned value may not be the same as in client side.
	 */
	public boolean isRaised() {
		return getElement().getProperty("raised", false);
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * If true, the button should be styled with a shadow.
	 * 
	 * @param raised
	 *            the boolean value to set
	 * @return this instance, for method chaining
	 */
	public R setRaised(boolean raised) {
		getElement().setProperty("raised", raised);
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
	 * Ensures this element contains a ripple effect. For startup efficiency the
	 * ripple effect is dynamically on demand when needed.
	 * 
	 * @param optTriggeringEvent
	 *            (optional) event that triggered the ripple.
	 */
	protected void ensureRipple(elemental.json.JsonObject optTriggeringEvent) {
		getElement().callFunction("ensureRipple", optTriggeringEvent);
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Returns the {@code <paper-ripple>} element used by this element to create
	 * ripple effects. The element's ripple is created on demand, when
	 * necessary, and calling this method will force the ripple to be created.
	 */
	public void getRipple() {
		getElement().callFunction("getRipple");
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Returns true if this element currently contains a ripple effect.
	 * 
	 * @return It would return a boolean
	 */
	@NotSupported
	protected void hasRipple() {
	}

	@DomEvent("active-changed")
	public static class ActiveChangeEvent<R extends GeneratedPaperButton<R>>
			extends
				ComponentEvent<R> {
		public ActiveChangeEvent(R source, boolean fromClient) {
			super(source, fromClient);
		}
	}

	public Registration addActiveChangeListener(
			ComponentEventListener<ActiveChangeEvent<R>> listener) {
		return addListener(ActiveChangeEvent.class,
				(ComponentEventListener) listener);
	}

	@DomEvent("focused-changed")
	public static class FocusedChangeEvent<R extends GeneratedPaperButton<R>>
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
	public static class DisabledChangeEvent<R extends GeneratedPaperButton<R>>
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

	@DomEvent("transitionend")
	public static class TransitionendEvent<R extends GeneratedPaperButton<R>>
			extends
				ComponentEvent<R> {
		public TransitionendEvent(R source, boolean fromClient) {
			super(source, fromClient);
		}
	}

	public Registration addTransitionendListener(
			ComponentEventListener<TransitionendEvent<R>> listener) {
		return addListener(TransitionendEvent.class,
				(ComponentEventListener) listener);
	}

	/**
	 * Sets the given string as the content of this component.
	 * 
	 * @param the
	 *            text content to set
	 * @see HasText#setText(String)
	 */
	public GeneratedPaperButton(java.lang.String text) {
		setText(text);
	}

	/**
	 * Default constructor.
	 */
	public GeneratedPaperButton() {
	}
}