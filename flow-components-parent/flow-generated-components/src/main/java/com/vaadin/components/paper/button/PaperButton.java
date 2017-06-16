package com.vaadin.components.paper.button;

import com.vaadin.ui.Component;
import javax.annotation.Generated;
import com.vaadin.annotations.Tag;
import com.vaadin.annotations.HtmlImport;
import elemental.json.JsonObject;
import com.vaadin.shared.Registration;
import com.vaadin.flow.dom.DomEventListener;

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
		"Generator: com.vaadin.generator.ComponentGenerator#0.1.10-SNAPSHOT",
		"WebComponent: paper-button#2.0.0", "Flow#0.1.10-SNAPSHOT"})
@Tag("paper-button")
@HtmlImport("frontend://bower_components/paper-button/paper-button.html")
public class PaperButton extends Component {

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * The EventTarget that will be firing relevant KeyboardEvents. Set it to
	 * {@code null} to disable the listeners.
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
	 */
	public void setKeyEventTarget(elemental.json.JsonObject keyEventTarget) {
		getElement().setPropertyJson("keyEventTarget", keyEventTarget);
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * If true, this property will cause the implementing element to
	 * automatically stop propagation on any handled KeyboardEvents.
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
	 */
	public void setStopKeyboardEventPropagation(
			boolean stopKeyboardEventPropagation) {
		getElement().setProperty("stopKeyboardEventPropagation",
				stopKeyboardEventPropagation);
	}

	public JsonObject getKeyBindings() {
		return (JsonObject) getElement().getPropertyRaw("keyBindings");
	}

	/**
	 * @param keyBindings
	 */
	public void setKeyBindings(elemental.json.JsonObject keyBindings) {
		getElement().setPropertyJson("keyBindings", keyBindings);
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * If true, the user is currently holding down the button.
	 */
	public boolean isPressed() {
		return getElement().getProperty("pressed", false);
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * If true, the user is currently holding down the button.
	 * 
	 * @param pressed
	 */
	public void setPressed(boolean pressed) {
		getElement().setProperty("pressed", pressed);
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * If true, the button toggles the active state with each tap or press of
	 * the spacebar.
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
	 */
	public void setToggles(boolean toggles) {
		getElement().setProperty("toggles", toggles);
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * If true, the button is a toggle and is currently in the active state.
	 */
	public boolean isActive() {
		return getElement().getProperty("active", false);
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * If true, the button is a toggle and is currently in the active state.
	 * 
	 * @param active
	 */
	public void setActive(boolean active) {
		getElement().setProperty("active", active);
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * True if the element is currently being pressed by a "pointer," which is
	 * loosely defined as mouse or touch input (but specifically excluding
	 * keyboard input).
	 */
	public boolean isPointerDown() {
		return getElement().getProperty("pointerDown", false);
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * True if the element is currently being pressed by a "pointer," which is
	 * loosely defined as mouse or touch input (but specifically excluding
	 * keyboard input).
	 * 
	 * @param pointerDown
	 */
	public void setPointerDown(boolean pointerDown) {
		getElement().setProperty("pointerDown", pointerDown);
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * True if the input device that caused the element to receive focus was a
	 * keyboard.
	 */
	public boolean isReceivedFocusFromKeyboard() {
		return getElement().getProperty("receivedFocusFromKeyboard", false);
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * True if the input device that caused the element to receive focus was a
	 * keyboard.
	 * 
	 * @param receivedFocusFromKeyboard
	 */
	public void setReceivedFocusFromKeyboard(boolean receivedFocusFromKeyboard) {
		getElement().setProperty("receivedFocusFromKeyboard",
				receivedFocusFromKeyboard);
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * The aria attribute to be set if the button is a toggle and in the active
	 * state.
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
	 */
	public void setAriaActiveAttribute(java.lang.String ariaActiveAttribute) {
		getElement().setProperty("ariaActiveAttribute",
				ariaActiveAttribute == null ? "" : ariaActiveAttribute);
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * If true, the element currently has focus.
	 */
	public boolean isFocused() {
		return getElement().getProperty("focused", false);
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * If true, the element currently has focus.
	 * 
	 * @param focused
	 */
	public void setFocused(boolean focused) {
		getElement().setProperty("focused", focused);
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * If true, the user cannot interact with this element.
	 */
	public boolean isDisabled() {
		return getElement().getProperty("disabled", false);
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * If true, the user cannot interact with this element.
	 * 
	 * @param disabled
	 */
	public void setDisabled(boolean disabled) {
		getElement().setProperty("disabled", disabled);
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * If true, the element will not produce a ripple effect when interacted
	 * with via the pointer.
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
	 */
	public void setNoink(boolean noink) {
		getElement().setProperty("noink", noink);
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * The z-depth of this element, from 0-5. Setting to 0 will remove the
	 * shadow, and each increasing number greater than 0 will be "deeper" than
	 * the last.
	 */
	public double getElevation() {
		return getElement().getProperty("elevation", 0.0);
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * The z-depth of this element, from 0-5. Setting to 0 will remove the
	 * shadow, and each increasing number greater than 0 will be "deeper" than
	 * the last.
	 * 
	 * @param elevation
	 */
	public void setElevation(double elevation) {
		getElement().setProperty("elevation", elevation);
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * If true, the button should be styled with a shadow.
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
	 */
	public void setRaised(boolean raised) {
		getElement().setProperty("raised", raised);
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
	 */
	public void keyboardEventMatchesKeys(elemental.json.JsonObject event,
			java.lang.String eventString) {
		getElement().callFunction("keyboardEventMatchesKeys", event,
				eventString);
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Ensures this element contains a ripple effect. For startup efficiency the
	 * ripple effect is dynamically on demand when needed.
	 * 
	 * @param optTriggeringEvent
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
	 */
	public void hasRipple() {
		getElement().callFunction("hasRipple");
	}

	public Registration addActiveChangedListener(DomEventListener listener) {
		return getElement().addEventListener("active-changed", listener);
	}

	public Registration addFocusedChangedListener(
			com.vaadin.flow.dom.DomEventListener listener) {
		return getElement().addEventListener("focused-changed", listener);
	}

	public Registration addDisabledChangedListener(
			com.vaadin.flow.dom.DomEventListener listener) {
		return getElement().addEventListener("disabled-changed", listener);
	}

	public Registration addTransitionendListener(
			com.vaadin.flow.dom.DomEventListener listener) {
		return getElement().addEventListener("transitionend", listener);
	}
}