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
package com.vaadin.ui.paper.tabs;

import com.vaadin.ui.Component;
import com.vaadin.ui.common.ComponentSupplier;
import com.vaadin.ui.common.HasStyle;
import javax.annotation.Generated;
import com.vaadin.ui.Tag;
import com.vaadin.ui.common.HtmlImport;
import com.vaadin.ui.event.Synchronize;
import elemental.json.JsonObject;
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
 * {@code paper-tab} is styled to look like a tab. It should be used in
 * conjunction with {@code paper-tabs}.
 * </p>
 * <p>
 * Example:
 * </p>
 * 
 * <pre>
 * <code>&lt;paper-tabs selected=&quot;0&quot;&gt;
 *   &lt;paper-tab&gt;TAB 1&lt;/paper-tab&gt;
 *   &lt;paper-tab&gt;TAB 2&lt;/paper-tab&gt;
 *   &lt;paper-tab&gt;TAB 3&lt;/paper-tab&gt;
 * &lt;/paper-tabs&gt;
 * </code>
 * </pre>
 * 
 * <h3>Styling</h3>
 * <p>
 * The following custom properties and mixins are available for styling:
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
 * <td>{@code --paper-tab-ink}</td>
 * <td>Ink color</td>
 * <td>{@code --paper-yellow-a100}</td>
 * </tr>
 * <tr>
 * <td>{@code --paper-tab}</td>
 * <td>Mixin applied to the tab</td>
 * <td>{@code</td>
 * </tr>
 * <tr>
 * <td>{@code --paper-tab-content}</td>
 * <td>Mixin applied to the tab content</td>
 * <td>{@code</td>
 * </tr>
 * <tr>
 * <td>{@code --paper-tab-content-unselected}</td>
 * <td>Mixin applied to the tab content when the tab is not selected</td>
 * <td>{@code</td>
 * </tr>
 * </tbody>
 * </table>
 * <p>
 * This element applies the mixin {@code --paper-font-common-base} but does not
 * import {@code paper-styles/typography.html}. In order to apply the
 * {@code Roboto} font to this element, make sure you've imported
 * {@code paper-styles/typography.html}.
 * </p>
 */
@Generated({"Generator: com.vaadin.generator.ComponentGenerator#1.0-SNAPSHOT",
		"WebComponent: paper-tab#2.0.0", "Flow#1.0-SNAPSHOT"})
@Tag("paper-tab")
@HtmlImport("frontend://bower_components/paper-tabs/paper-tab.html")
public class GeneratedPaperTab<R extends GeneratedPaperTab<R>>
		extends
			Component implements ComponentSupplier<R>, HasStyle, HasComponents {

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
	 * If true, the tab will forward keyboard clicks (enter/space) to the first
	 * anchor element found in its descendants
	 * <p>
	 * This property is not synchronized automatically from the client side, so
	 * the returned value may not be the same as in client side.
	 * </p>
	 * 
	 * @return the {@code link} property from the webcomponent
	 */
	public boolean isLink() {
		return getElement().getProperty("link", false);
	}

	/**
	 * <p>
	 * Description copied from corresponding location in WebComponent:
	 * </p>
	 * <p>
	 * If true, the tab will forward keyboard clicks (enter/space) to the first
	 * anchor element found in its descendants
	 * </p>
	 * 
	 * @param link
	 *            the boolean value to set
	 */
	public void setLink(boolean link) {
		getElement().setProperty("link", link);
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

	@DomEvent("focused-changed")
	public static class FocusedChangeEvent<R extends GeneratedPaperTab<R>>
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
	public static class DisabledChangeEvent<R extends GeneratedPaperTab<R>>
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

	@DomEvent("active-changed")
	public static class ActiveChangeEvent<R extends GeneratedPaperTab<R>>
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

	/**
	 * Adds the given components as children of this component.
	 * 
	 * @param components
	 *            the components to add
	 * @see HasComponents#add(Component...)
	 */
	public GeneratedPaperTab(com.vaadin.ui.Component... components) {
		add(components);
	}

	/**
	 * Default constructor.
	 */
	public GeneratedPaperTab() {
	}
}