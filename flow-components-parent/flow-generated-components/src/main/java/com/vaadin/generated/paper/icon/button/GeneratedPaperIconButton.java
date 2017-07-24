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
package com.vaadin.generated.paper.icon.button;

import com.vaadin.ui.Component;
import com.vaadin.ui.ComponentSupplier;
import com.vaadin.ui.HasStyle;
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

/**
 * 
 */
@Generated({
		"Generator: com.vaadin.generator.ComponentGenerator#0.1.14-SNAPSHOT",
		"WebComponent: paper-icon-button#2.0.0", "Flow#0.1.14-SNAPSHOT"})
@Tag("paper-icon-button")
@HtmlImport("frontend://bower_components/paper-icon-button/paper-icon-button.html")
public class GeneratedPaperIconButton<R extends GeneratedPaperIconButton<R>>
		extends
			Component implements ComponentSupplier<R>, HasStyle {

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
	public R setKeyEventTarget(elemental.json.JsonObject keyEventTarget) {
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
	public JsonObject getKeyBindings() {
		return (JsonObject) getElement().getPropertyRaw("keyBindings");
	}

	/**
	 * @param keyBindings
	 *            the JsonObject value to set
	 * @return this instance, for method chaining
	 */
	public R setKeyBindings(elemental.json.JsonObject keyBindings) {
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
	 * The URL of an image for the icon. If the src property is specified, the
	 * icon property should not be.
	 * <p>
	 * This property is not synchronized automatically from the client side, so
	 * the returned value may not be the same as in client side.
	 */
	public String getSrc() {
		return getElement().getProperty("src");
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * The URL of an image for the icon. If the src property is specified, the
	 * icon property should not be.
	 * 
	 * @param src
	 *            the String value to set
	 * @return this instance, for method chaining
	 */
	public R setSrc(java.lang.String src) {
		getElement().setProperty("src", src == null ? "" : src);
		return get();
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Specifies the icon name or index in the set of icons available in the
	 * icon's icon set. If the icon property is specified, the src property
	 * should not be.
	 * <p>
	 * This property is not synchronized automatically from the client side, so
	 * the returned value may not be the same as in client side.
	 */
	public String getIcon() {
		return getElement().getProperty("icon");
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Specifies the icon name or index in the set of icons available in the
	 * icon's icon set. If the icon property is specified, the src property
	 * should not be.
	 * 
	 * @param icon
	 *            the String value to set
	 * @return this instance, for method chaining
	 */
	public R setIcon(java.lang.String icon) {
		getElement().setProperty("icon", icon == null ? "" : icon);
		return get();
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Specifies the alternate text for the button, for accessibility.
	 * <p>
	 * This property is not synchronized automatically from the client side, so
	 * the returned value may not be the same as in client side.
	 */
	public String getAlt() {
		return getElement().getProperty("alt");
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Specifies the alternate text for the button, for accessibility.
	 * 
	 * @param alt
	 *            the String value to set
	 * @return this instance, for method chaining
	 */
	public R setAlt(java.lang.String alt) {
		getElement().setProperty("alt", alt == null ? "" : alt);
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
	public void ensureRipple(elemental.json.JsonObject optTriggeringEvent) {
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
	public static class ActiveChangeEvent
			extends
				ComponentEvent<GeneratedPaperIconButton> {
		public ActiveChangeEvent(GeneratedPaperIconButton source,
				boolean fromClient) {
			super(source, fromClient);
		}
	}

	public Registration addActiveChangeListener(
			ComponentEventListener<ActiveChangeEvent> listener) {
		return addListener(ActiveChangeEvent.class, listener);
	}

	@DomEvent("focused-changed")
	public static class FocusedChangeEvent
			extends
				ComponentEvent<GeneratedPaperIconButton> {
		public FocusedChangeEvent(GeneratedPaperIconButton source,
				boolean fromClient) {
			super(source, fromClient);
		}
	}

	public Registration addFocusedChangeListener(
			ComponentEventListener<FocusedChangeEvent> listener) {
		return addListener(FocusedChangeEvent.class, listener);
	}

	@DomEvent("disabled-changed")
	public static class DisabledChangeEvent
			extends
				ComponentEvent<GeneratedPaperIconButton> {
		public DisabledChangeEvent(GeneratedPaperIconButton source,
				boolean fromClient) {
			super(source, fromClient);
		}
	}

	public Registration addDisabledChangeListener(
			ComponentEventListener<DisabledChangeEvent> listener) {
		return addListener(DisabledChangeEvent.class, listener);
	}
}