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
package com.vaadin.ui.paper.iconbutton;

import com.vaadin.ui.Component;
import com.vaadin.ui.common.ComponentSupplier;
import com.vaadin.ui.common.HasStyle;
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

/**
 * <p>
 * Description copied from corresponding location in WebComponent:
 * </p>
 * <p>
 * Material design: <a href=
 * "https://www.google.com/design/spec/components/buttons.html#buttons-toggle-buttons"
 * >Icon toggles</a>
 * </p>
 * <p>
 * {@code paper-icon-button} is a button with an image placed at the center.
 * When the user touches the button, a ripple effect emanates from the center of
 * the button.
 * </p>
 * <p>
 * {@code paper-icon-button} does not include a default icon set. To use icons
 * from the default set, include
 * {@code PolymerElements/iron-icons/iron-icons.html}, and use the {@code icon}
 * attribute to specify which icon from the icon set to use.
 * </p>
 * 
 * <pre>
 * <code>&lt;paper-icon-button icon=&quot;menu&quot;&gt;&lt;/paper-icon-button&gt;
 * </code>
 * </pre>
 * <p>
 * See <a href="iron-iconset">{@code iron-iconset}</a> for more information
 * about how to use a custom icon set.
 * </p>
 * <p>
 * Example:
 * </p>
 * 
 * <pre>
 * <code>&lt;link href=&quot;path/to/iron-icons/iron-icons.html&quot; rel=&quot;import&quot;&gt;
 * 
 * &lt;paper-icon-button icon=&quot;favorite&quot;&gt;&lt;/paper-icon-button&gt;
 * &lt;paper-icon-button src=&quot;star.png&quot;&gt;&lt;/paper-icon-button&gt;
 * </code>
 * </pre>
 * <p>
 * To use {@code paper-icon-button} as a link, wrap it in an anchor tag. Since
 * {@code paper-icon-button} will already receive focus, you may want to prevent
 * the anchor tag from receiving focus as well by setting its tabindex to -1.
 * </p>
 * 
 * <pre>
 * <code>&lt;a href=&quot;https://www.polymer-project.org&quot; tabindex=&quot;-1&quot;&gt;
 *   &lt;paper-icon-button icon=&quot;polymer&quot;&gt;&lt;/paper-icon-button&gt;
 * &lt;/a&gt;
 * </code>
 * </pre>
 * 
 * <h3>Styling</h3>
 * <p>
 * Style the button with CSS as you would a normal DOM element. If you are using
 * the icons provided by {@code iron-icons}, they will inherit the foreground
 * color of the button.
 * </p>
 * 
 * <pre>
 * <code>/* make a red &quot;favorite&quot; button &#42;&#47;
 * &lt;paper-icon-button icon=&quot;favorite&quot; style=&quot;color: red;&quot;&gt;&lt;/paper-icon-button&gt;
 * </code>
 * </pre>
 * <p>
 * By default, the ripple is the same color as the foreground at 25% opacity.
 * You may customize the color using the {@code --paper-icon-button-ink-color}
 * custom property.
 * </p>
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
 * <td>{@code --paper-icon-button-disabled-text}</td>
 * <td>The color of the disabled button</td>
 * <td>{@code --disabled-text-color}</td>
 * </tr>
 * <tr>
 * <td>{@code --paper-icon-button-ink-color}</td>
 * <td>Selected/focus ripple color</td>
 * <td>{@code --primary-text-color}</td>
 * </tr>
 * <tr>
 * <td>{@code --paper-icon-button}</td>
 * <td>Mixin for a button</td>
 * <td>{@code</td>
 * </tr>
 * <tr>
 * <td>{@code --paper-icon-button-disabled}</td>
 * <td>Mixin for a disabled button</td>
 * <td>{@code</td>
 * </tr>
 * <tr>
 * <td>{@code --paper-icon-button-hover}</td>
 * <td>Mixin for button on hover</td>
 * <td>{@code</td>
 * </tr>
 * </tbody>
 * </table>
 */
@Generated({"Generator: com.vaadin.generator.ComponentGenerator#1.0-SNAPSHOT",
		"WebComponent: paper-icon-button#2.0.1", "Flow#1.0-SNAPSHOT"})
@Tag("paper-icon-button")
@HtmlImport("frontend://bower_components/paper-icon-button/paper-icon-button.html")
public class GeneratedPaperIconButton<R extends GeneratedPaperIconButton<R>>
		extends
			Component implements ComponentSupplier<R>, HasStyle {

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
	 * The URL of an image for the icon. If the src property is specified, the
	 * icon property should not be.
	 * <p>
	 * This property is not synchronized automatically from the client side, so
	 * the returned value may not be the same as in client side.
	 * </p>
	 * 
	 * @return the {@code src} property from the webcomponent
	 */
	public String getSrc() {
		return getElement().getProperty("src");
	}

	/**
	 * <p>
	 * Description copied from corresponding location in WebComponent:
	 * </p>
	 * <p>
	 * The URL of an image for the icon. If the src property is specified, the
	 * icon property should not be.
	 * </p>
	 * 
	 * @param src
	 *            the String value to set
	 */
	public void setSrc(java.lang.String src) {
		getElement().setProperty("src", src == null ? "" : src);
	}

	/**
	 * <p>
	 * Description copied from corresponding location in WebComponent:
	 * </p>
	 * <p>
	 * Specifies the icon name or index in the set of icons available in the
	 * icon's icon set. If the icon property is specified, the src property
	 * should not be.
	 * <p>
	 * This property is not synchronized automatically from the client side, so
	 * the returned value may not be the same as in client side.
	 * </p>
	 * 
	 * @return the {@code icon} property from the webcomponent
	 */
	public String getIcon() {
		return getElement().getProperty("icon");
	}

	/**
	 * <p>
	 * Description copied from corresponding location in WebComponent:
	 * </p>
	 * <p>
	 * Specifies the icon name or index in the set of icons available in the
	 * icon's icon set. If the icon property is specified, the src property
	 * should not be.
	 * </p>
	 * 
	 * @param icon
	 *            the String value to set
	 */
	public void setIcon(java.lang.String icon) {
		getElement().setProperty("icon", icon == null ? "" : icon);
	}

	/**
	 * <p>
	 * Description copied from corresponding location in WebComponent:
	 * </p>
	 * <p>
	 * Specifies the alternate text for the button, for accessibility.
	 * <p>
	 * This property is not synchronized automatically from the client side, so
	 * the returned value may not be the same as in client side.
	 * </p>
	 * 
	 * @return the {@code alt} property from the webcomponent
	 */
	public String getAlt() {
		return getElement().getProperty("alt");
	}

	/**
	 * <p>
	 * Description copied from corresponding location in WebComponent:
	 * </p>
	 * <p>
	 * Specifies the alternate text for the button, for accessibility.
	 * </p>
	 * 
	 * @param alt
	 *            the String value to set
	 */
	public void setAlt(java.lang.String alt) {
		getElement().setProperty("alt", alt == null ? "" : alt);
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
	public static class ActiveChangeEvent<R extends GeneratedPaperIconButton<R>>
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
	public static class FocusedChangeEvent<R extends GeneratedPaperIconButton<R>>
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
	public static class DisabledChangeEvent<R extends GeneratedPaperIconButton<R>>
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
}