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
package com.vaadin.ui.paper.button;

import com.vaadin.ui.Component;
import com.vaadin.ui.common.ComponentSupplier;
import com.vaadin.ui.common.HasStyle;
import com.vaadin.ui.common.HasText;
import com.vaadin.ui.common.Focusable;
import com.vaadin.ui.common.HasClickListeners;
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
import com.vaadin.ui.common.HasComponents;

/**
 * <p>
 * Description copied from corresponding location in WebComponent:
 * </p>
 * <p>
 * Material design: <a
 * href="https://www.google.com/design/spec/components/buttons.html">Buttons</a>
 * </p>
 * <p>
 * {@code paper-button} is a button. When the user touches the button, a ripple
 * effect emanates from the point of contact. It may be flat or raised. A raised
 * button is styled with a shadow.
 * </p>
 * <p>
 * Example:
 * </p>
 * 
 * <pre>
 * <code>&lt;paper-button&gt;Flat button&lt;/paper-button&gt;
 * &lt;paper-button raised&gt;Raised button&lt;/paper-button&gt;
 * &lt;paper-button noink&gt;No ripple effect&lt;/paper-button&gt;
 * &lt;paper-button toggles&gt;Toggle-able button&lt;/paper-button&gt;
 * </code>
 * </pre>
 * <p>
 * A button that has {@code toggles} true will remain {@code active} after being
 * clicked (and will have an {@code active} attribute set). For more
 * information, see the {@code Polymer.IronButtonState} behavior.
 * </p>
 * <p>
 * You may use custom DOM in the button body to create a variety of buttons. For
 * example, to create a button with an icon and some text:
 * </p>
 * 
 * <pre>
 * <code>&lt;paper-button&gt;
 *   &lt;iron-icon icon=&quot;favorite&quot;&gt;&lt;/iron-icon&gt;
 *   custom button content
 * &lt;/paper-button&gt;
 * </code>
 * </pre>
 * <p>
 * To use {@code paper-button} as a link, wrap it in an anchor tag. Since
 * {@code paper-button} will already receive focus, you may want to prevent the
 * anchor tag from receiving focus as well by setting its tabindex to -1.
 * </p>
 * 
 * <pre>
 * <code>&lt;a href=&quot;https://www.polymer-project.org/&quot; tabindex=&quot;-1&quot;&gt;
 *   &lt;paper-button raised&gt;Polymer Project&lt;/paper-button&gt;
 * &lt;/a&gt;
 * </code>
 * </pre>
 * 
 * <h3>Styling</h3>
 * <p>
 * Style the button with CSS as you would a normal DOM element.
 * </p>
 * 
 * <pre>
 * <code>paper-button.fancy {
 *   background: green;
 *   color: yellow;
 * }
 * 
 * paper-button.fancy:hover {
 *   background: lime;
 * }
 * 
 * paper-button[disabled],
 * paper-button[toggles][active] {
 *   background: red;
 * }
 * </code>
 * </pre>
 * <p>
 * By default, the ripple is the same color as the foreground at 25% opacity.
 * You may customize the color using the {@code --paper-button-ink-color} custom
 * property.
 * </p>
 * <p>
 * The following custom properties and mixins are also available for styling:
 * </p>
 * <table>
 * <thead>
 * <tr>
 * <th>Custom property</th>
 * <th>Description</th>
 * <th>Default</th>
 * </tr>
 * </thead> <tbody>
 * <tr>
 * <td>{@code --paper-button-ink-color}</td>
 * <td>Background color of the ripple</td>
 * <td>{@code Based on the button's color}</td>
 * </tr>
 * <tr>
 * <td>{@code --paper-button}</td>
 * <td>Mixin applied to the button</td>
 * <td>{@code</td>
 * </tr>
 * <tr>
 * <td>{@code --paper-button-disabled}</td>
 * <td>Mixin applied to the disabled button. Note that you can also use the
 * {@code paper-button[disabled]} selector</td>
 * <td>{@code</td>
 * </tr>
 * <tr>
 * <td>{@code --paper-button-flat-keyboard-focus}</td>
 * <td>Mixin applied to a flat button after it's been focused using the keyboard
 * </td>
 * <td>{@code</td>
 * </tr>
 * <tr>
 * <td>{@code --paper-button-raised-keyboard-focus}</td>
 * <td>Mixin applied to a raised button after it's been focused using the
 * keyboard</td>
 * <td>{@code</td>
 * </tr>
 * </tbody>
 * </table>
 */
@Generated({"Generator: com.vaadin.generator.ComponentGenerator#1.0-SNAPSHOT",
		"WebComponent: paper-button#2.0.0", "Flow#1.0-SNAPSHOT"})
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
	protected void setKeyEventTarget(elemental.json.JsonObject keyEventTarget) {
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
	protected void setKeyBindings(elemental.json.JsonObject keyBindings) {
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
	public void setAriaActiveAttribute(java.lang.String ariaActiveAttribute) {
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
	 * The z-depth of this element, from 0-5. Setting to 0 will remove the
	 * shadow, and each increasing number greater than 0 will be
	 * &quot;deeper&quot; than the last.
	 * <p>
	 * This property is not synchronized automatically from the client side, so
	 * the returned value may not be the same as in client side.
	 * </p>
	 * 
	 * @return the {@code elevation} property from the webcomponent
	 */
	public double getElevation() {
		return getElement().getProperty("elevation", 0.0);
	}

	/**
	 * <p>
	 * Description copied from corresponding location in WebComponent:
	 * </p>
	 * <p>
	 * If true, the button should be styled with a shadow.
	 * <p>
	 * This property is not synchronized automatically from the client side, so
	 * the returned value may not be the same as in client side.
	 * </p>
	 * 
	 * @return the {@code raised} property from the webcomponent
	 */
	public boolean isRaised() {
		return getElement().getProperty("raised", false);
	}

	/**
	 * <p>
	 * Description copied from corresponding location in WebComponent:
	 * </p>
	 * <p>
	 * If true, the button should be styled with a shadow.
	 * </p>
	 * 
	 * @param raised
	 *            the boolean value to set
	 */
	public void setRaised(boolean raised) {
		getElement().setProperty("raised", raised);
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
	public void addOwnKeyBinding(java.lang.String eventString,
			java.lang.String handlerName) {
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
	protected void keyboardEventMatchesKeys(elemental.json.JsonObject event,
			java.lang.String eventString) {
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
	protected void ensureRipple(elemental.json.JsonObject optTriggeringEvent) {
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